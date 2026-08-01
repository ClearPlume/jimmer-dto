package net.fallingangel.jimmerdto.psi.resolve

import com.intellij.psi.*
import net.fallingangel.jimmerdto.enums.StandardType
import net.fallingangel.jimmerdto.lsi.LClass
import net.fallingangel.jimmerdto.lsi.LProperty
import net.fallingangel.jimmerdto.lsi.compiling
import net.fallingangel.jimmerdto.psi.DTOFile
import net.fallingangel.jimmerdto.util.ktClass
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtEnumEntry

object Resolution {
    sealed class Space {
        abstract fun resolve(name: String): Target?

        class GlobalRaw(val file: DTOFile) : Space() {
            override fun resolve(name: String): Target? {
                val scope = file.resolveScope
                val facade = JavaPsiFacade.getInstance(file.project)
                return facade.findClass(name, scope)?.let { Target.Type(it) }
                    ?: facade.findPackage(name)?.let(Target::Pkg)
                    // Java 索引看不见 Kotlin builtins（kotlin.Int 等无类文件），两个索引并查才是完整的包内容
                    ?: file.ktClass(name)?.let { Target.Type(it) }
            }
        }

        class GlobalWithImports(val file: DTOFile, val fallbackPackage: String) : Space() {
            private val global = GlobalRaw(file)

            override fun resolve(name: String): Target? {
                // 用户属性输入时：`user: <光标>`
                if (name.isEmpty()) {
                    return null
                }

                StandardType[name]?.let { standard ->
                    // 内置类型截断在此：无论有无可跳转的声明都不再向下
                    // Java 侧的 Array 无对应类，source 为 null，引用不解析
                    val type = compiling(file) { builtinType(standard) } ?: return null
                    return Target.Type(type)
                }

                val candidates = file.importIndex[name]
                if (candidates != null) {
                    // 歧义：错误已报在 import 处，此处静默
                    val qualified = candidates.singleOrNull() ?: return null
                    return global.resolve(qualified)
                }

                if (name.first().isLowerCase()) {
                    return global.resolve(name)
                }

                val defaultQualified = if (fallbackPackage.isEmpty()) name else "$fallbackPackage.$name"
                return global.resolve(defaultQualified)
            }
        }

        class Type(val declaration: PsiElement) : Space() {
            override fun resolve(name: String): Target? {
                return when (declaration) {
                    is PsiClass if declaration !is KtLightClass -> {
                        val field = declaration.findFieldByName(name, false)
                        // 普通 static 字段作为中间段没有后继，且不该存在 Target.Field
                        (field as? PsiEnumConstant)?.let(Target::EnumConst)
                            ?: declaration.findInnerClassByName(name, false)?.let { Target.Type(it) }
                    }

                    is KtClass if declaration !is KtEnumEntry -> {
                        val members = declaration.body?.declarations.orEmpty()
                        members.filterIsInstance<KtEnumEntry>().find { it.name == name }?.let(Target::EnumConst)
                            ?: members.filterIsInstance<KtClassOrObject>()
                                .find { it.name == name }
                                ?.let { Target.Type(it) }
                    }

                    else -> null
                }
            }
        }

        class Pkg(val `package`: PsiPackage) : Space() {
            override fun resolve(name: String): Target? {
                return `package`.subPackages.find { it.name == name }?.let(Target::Pkg)
                    ?: `package`.classes.find { it.name == name }?.let { Target.Type(it) }
                    // Java 索引看不见 Kotlin builtins（kotlin.Int 等无类文件），两个索引并查才是完整的包内容
                    ?: `package`.ktClass("${`package`.qualifiedName}.$name")?.let { Target.Type(it) }
            }
        }

        class Properties(val clazz: LClass) : Space() {
            override fun resolve(name: String): Target? {
                val property = clazz.findProperty(name) ?: clazz.findProperty(name.removeSuffix("Id"))
                return property?.let(Target::Property)
            }
        }
    }

    sealed class Target {
        abstract val source: PsiElement?

        abstract fun spaceForMembers(): Space?

        class Pkg(val `package`: PsiPackage) : Target() {
            override val source: PsiElement
                get() = `package`

            override fun spaceForMembers(): Space {
                return Space.Pkg(`package`)
            }
        }

        class Type private constructor(val type: PsiElement) : Target() {
            companion object {
                operator fun invoke(type: PsiElement): Target {
                    return Type((type as? KtLightClass)?.kotlinOrigin ?: type)
                }
            }

            override val source: PsiElement
                get() = type

            override fun spaceForMembers(): Space {
                return Space.Type(type)
            }
        }

        class Property(val property: LProperty) : Target() {
            override val source: PsiElement?
                get() = property.source

            override fun spaceForMembers(): Space? {
                return property.targetClass?.let(Space::Properties)
            }
        }

        class EnumConst(val enum: PsiElement) : Target() {
            override val source: PsiElement
                get() = enum

            override fun spaceForMembers(): Space? {
                return null
            }
        }
    }
}
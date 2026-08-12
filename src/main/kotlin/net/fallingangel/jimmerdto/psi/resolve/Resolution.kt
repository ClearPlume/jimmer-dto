package net.fallingangel.jimmerdto.psi.resolve

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPackage
import net.fallingangel.jimmerdto.enums.StandardType
import net.fallingangel.jimmerdto.lsi.LClass
import net.fallingangel.jimmerdto.lsi.LProperty
import net.fallingangel.jimmerdto.lsi.compiling
import net.fallingangel.jimmerdto.lsi.jimmer.defaultViewBasePropName
import net.fallingangel.jimmerdto.lsi.jimmer.idProperty
import net.fallingangel.jimmerdto.lsi.jimmer.idViewBaseProp
import net.fallingangel.jimmerdto.lsi.jimmer.isReference
import net.fallingangel.jimmerdto.lsi.process
import net.fallingangel.jimmerdto.psi.DTOFile
import net.fallingangel.jimmerdto.util.ktClass
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.idea.stubindex.KotlinTopLevelClassByPackageIndex

object Resolution {
    sealed class Space {
        abstract fun resolve(name: String): Target?
        abstract fun candidates(): List<Candidate>

        class GlobalRaw(val file: DTOFile) : Space() {
            override fun resolve(name: String): Target? {
                val facade = JavaPsiFacade.getInstance(file.project)
                return facade.findClass(name, file.resolveScope)?.takeIf { it !is KtLightElement<*, *> }?.let { Target.Type(it) }
                    ?: facade.findPackage(name)?.let(Target::Pkg)
                    ?: file.ktClass(name)?.let { Target.Type(it) }
            }

            override fun candidates(): List<Candidate> {
                return JavaPsiFacade.getInstance(file.project)
                    .findPackage("")
                    ?.subPackages
                    ?.map {
                        Candidate(
                            requireNotNull(it.name) { "Sub package of root has no name: $it" },
                            Target.Pkg(it),
                        )
                    }
                    .orEmpty()
            }
        }

        class GlobalWithImports(val file: DTOFile, val fallbackPackage: String) : Space() {
            private val global = GlobalRaw(file)

            override fun resolve(name: String): Target? {
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

            override fun candidates(): List<Candidate> {
                val standardTargets = StandardType.entries.mapNotNull {
                    val type = compiling(file) { builtinType(it) } ?: return@mapNotNull null
                    Candidate(it.name, Target.Type(type))
                }

                val importedTargets = file.importIndex.flatMap { (name, candidates) ->
                    candidates.mapNotNull { qualified ->
                        val target = global.resolve(qualified) ?: return@mapNotNull null
                        Candidate(name, target)
                    }
                }

                val topLevelTargets = global.candidates()

                val fallbackTargets = JavaPsiFacade.getInstance(file.project)
                    .findPackage(fallbackPackage)
                    ?.let { psiPackage -> Pkg(psiPackage).candidates().filter { (_, target) -> target is Target.Type } }
                    .orEmpty()

                return standardTargets + importedTargets + topLevelTargets + fallbackTargets
            }
        }

        class Type(val declaration: PsiElement) : Space() {
            override fun resolve(name: String): Target? {
                return candidates().find { it.name == name }?.target
            }

            override fun candidates(): List<Candidate> {
                return process(declaration) {
                    val nestedTypes = nestedTypes().mapNotNull { Candidate(it.name ?: return@mapNotNull null, Target.Type(it)) }
                    val enumConstants = enumConstants().mapNotNull { Candidate(it.name ?: return@mapNotNull null, Target.EnumConst(it)) }
                    nestedTypes + enumConstants
                }.orEmpty()
            }
        }

        class Pkg(val `package`: PsiPackage) : Space() {
            override fun resolve(name: String): Target? {
                return `package`.subPackages.find { it.name == name }?.let(Target::Pkg)
                    ?: `package`.classes.find { it !is KtLightElement<*, *> && it.name == name }?.let { Target.Type(it) }
                    ?: KotlinTopLevelClassByPackageIndex[`package`.qualifiedName, `package`.project, `package`.resolveScope]
                        .find { it.name == name }?.let { Target.Type(it) }
            }

            override fun candidates(): List<Candidate> {
                val subPackages = `package`.subPackages.map {
                    Candidate(
                        requireNotNull(it.name) { "Sub package has no name: $it" },
                        Target.Pkg(it),
                    )
                }

                val classes = `package`.classes.filter { it !is KtLightElement<*, *> }.map {
                    Candidate(
                        requireNotNull(it.name) { "PsiClass has no name: $it" },
                        Target.Type(it),
                    )
                }

                val ktClasses = KotlinTopLevelClassByPackageIndex[`package`.qualifiedName, `package`.project, `package`.resolveScope]
                    .mapNotNull { it.name?.let { name -> Candidate(name, Target.Type(it)) } }

                return subPackages + classes + ktClasses
            }
        }

        class Properties(val clazz: LClass) : Space() {
            override fun resolve(name: String): Target? {
                clazz.findProperty(name)?.let { prop ->
                    val via = prop.idViewBaseProp?.let { Target.Property.Via.IdView(it) }
                    return Target.Property(prop, via)
                }

                val base = defaultViewBasePropName(name, false) ?: return null
                val reference = clazz.findProperty(base)?.takeIf { it.isReference } ?: return null

                return reference.targetClass?.idProperty?.let { Target.Property(it, Target.Property.Via.ImplicitId(reference)) }
            }

            override fun candidates(): List<Candidate> {
                val explicit = clazz.allProperties
                    .map {
                        Candidate(
                            it.name,
                            Target.Property(it, it.idViewBaseProp?.let(Target.Property.Via::IdView)),
                        )
                    }
                val taken = explicit.mapTo(mutableSetOf()) { it.name }

                val implicit = clazz.allProperties
                    .filter { it.isReference }
                    .mapNotNull { ref ->
                        val name = "${ref.name}Id".takeIf { it !in taken } ?: return@mapNotNull null
                        ref.targetClass
                            ?.idProperty
                            ?.let { Candidate(name, Target.Property(it, Target.Property.Via.ImplicitId(ref))) }
                    }

                return explicit + implicit
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

        class Property(val property: LProperty, val via: Via? = null) : Target() {
            sealed class Via {
                class IdView(val reference: LProperty) : Via()
                class ImplicitId(val reference: LProperty) : Via()
            }

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

    data class Candidate(val name: String, val target: Target)
}
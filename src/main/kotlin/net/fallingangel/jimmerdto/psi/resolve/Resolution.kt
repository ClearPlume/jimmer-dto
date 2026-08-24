package net.fallingangel.jimmerdto.psi.resolve

import com.intellij.codeInsight.completion.AllClassesGetter
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiPackage
import net.fallingangel.jimmerdto.enums.AUTO_IMPORTED_TYPES
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
import net.fallingangel.jimmerdto.psi.demand
import net.fallingangel.jimmerdto.psi.element.DTOAlias
import org.jetbrains.kotlin.asJava.unwrapped

object Resolution {
    sealed class Space {
        abstract fun resolve(name: String): Target?
        abstract fun candidates(): List<Candidate>

        class GlobalRaw(val file: DTOFile) : Space() {
            override fun resolve(name: String): Target? {
                val facade = JavaPsiFacade.getInstance(file.project)
                return facade.findClass(name, file.resolveScope)?.unwrapped?.let { Target.Type(it as PsiNamedElement) }
                    ?: facade.findPackage(name)?.let { Target.Pkg(file, it) }
            }

            override fun candidates(): List<Candidate> {
                return JavaPsiFacade.getInstance(file.project)
                    .findPackage("")
                    ?.getSubPackages(file.resolveScope)
                    ?.map { Candidate(it.demand(PsiPackage::getName), Target.Pkg(file, it)) }
                    .orEmpty()
            }

            context(parameters: CompletionParameters, matcher: PrefixMatcher)
            fun eachClass(consume: (PsiNamedElement) -> Unit) {
                AllClassesGetter.processJavaClasses(parameters, matcher, true) { psiClass ->
                    if (psiClass.demand(PsiClass::getQualifiedName) in AUTO_IMPORTED_TYPES) return@processJavaClasses
                    consume(psiClass.unwrapped as? PsiNamedElement ?: return@processJavaClasses)
                }
            }
        }

        class GlobalWithImports(val file: DTOFile, val fallbackPackage: String) : Space() {
            val global = GlobalRaw(file)

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
                    val target = global.resolve(qualified.qualifiedName)

                    return if (qualified.alias == null) {
                        target
                    } else {
                        Target.Alias(qualified.alias, target as? Target.Type)
                    }
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
                        val target = global.resolve(qualified.qualifiedName) ?: return@mapNotNull null

                        if (qualified.alias == null) {
                            Candidate(name, target)
                        } else {
                            Candidate(name, Target.Alias(qualified.alias, target as? Target.Type))
                        }
                    }
                }

                val topLevelTargets = global.candidates()

                val fallbackTargets = JavaPsiFacade.getInstance(file.project)
                    .findPackage(fallbackPackage)
                    ?.let { psiPackage -> Pkg(file, psiPackage).candidates().filter { (_, target) -> target is Target.Type } }
                    .orEmpty()

                return standardTargets + importedTargets + topLevelTargets + fallbackTargets
            }
        }

        class Type(val declaration: PsiNamedElement) : Space() {
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

        class Pkg(val file: DTOFile, val `package`: PsiPackage) : Space() {
            override fun resolve(name: String): Target? {
                return `package`.getClasses(file.resolveScope).find { it.name == name }?.unwrapped?.let { Target.Type(it as PsiNamedElement) }
                    ?: `package`.getSubPackages(file.resolveScope).find { it.name == name }?.let { Target.Pkg(file, it) }
            }

            override fun candidates(): List<Candidate> {
                val subPackages = `package`.getSubPackages(file.resolveScope)
                    .map { Candidate(it.demand(PsiPackage::getName), Target.Pkg(file, it)) }

                val classes = `package`.getClasses(file.resolveScope)
                    .mapNotNull { it.unwrapped }
                    .map { it as PsiNamedElement }
                    .map { Candidate(it.demand(PsiNamedElement::getName), Target.Type(it)) }

                return subPackages + classes
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

        class Subtypes(val file: DTOFile, val lClass: LClass) : Space() {
            val global = GlobalWithImports(file, file.entityPackage)

            override fun resolve(name: String): Target? {
                if (lClass.name == name) return Target.Subtype(lClass)

                val child = lClass.children.find { it.name == name && it.lName.pkg == file.entityPackage }
                child?.let { return Target.Subtype(it) }

                return global.resolve(name)
            }

            override fun candidates(): List<Candidate> {
                return lClass.children.map { Candidate(it.name, Target.Subtype(it)) }
            }
        }
    }

    sealed class Target {
        abstract val source: PsiNamedElement

        abstract fun spaceForMembers(): Space?

        class Pkg(val file: DTOFile, val `package`: PsiPackage) : Target() {
            override val source: PsiNamedElement
                get() = `package`

            override fun spaceForMembers(): Space {
                return Space.Pkg(file, `package`)
            }
        }

        class Type(val type: PsiNamedElement) : Target() {
            override val source: PsiNamedElement
                get() = type

            override fun spaceForMembers(): Space {
                return Space.Type(type)
            }
        }

        class Alias(val alias: DTOAlias, val target: Type?) : Target() {
            override val source: PsiNamedElement
                get() = alias

            override fun spaceForMembers(): Space? {
                return target?.spaceForMembers()
            }
        }

        class Property(val property: LProperty, val via: Via? = null) : Target() {
            sealed class Via {
                class IdView(val reference: LProperty) : Via()
                class ImplicitId(val reference: LProperty) : Via()
            }

            override val source: PsiNamedElement
                get() = property.dependencyItem

            override fun spaceForMembers(): Space? {
                return property.targetClass?.let(Space::Properties)
            }
        }

        class Subtype(val lClass: LClass) : Target() {
            override val source: PsiNamedElement
                get() = lClass.dependencyItem

            override fun spaceForMembers(): Space? {
                return null
            }
        }

        class EnumConst(val enum: PsiNamedElement) : Target() {
            override val source: PsiNamedElement
                get() = enum

            override fun spaceForMembers(): Space? {
                return null
            }
        }
    }

    data class Candidate(val name: String, val target: Target)
}

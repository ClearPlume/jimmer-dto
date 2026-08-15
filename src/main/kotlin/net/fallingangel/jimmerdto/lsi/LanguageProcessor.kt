package net.fallingangel.jimmerdto.lsi

import com.intellij.lang.Language
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotation
import org.jetbrains.kotlin.name.ClassId

private val EP = ExtensionPointName.create<LanguageProcessor>("net.fallingangel.languageProcessor")

interface LanguageProcessor {
    fun supports(language: Language): Boolean

    context(element: PsiElement, types: ResolvedTypes)
    fun lClass(): LClass?

    context(element: PsiElement, types: ResolvedTypes)
    fun lProperty(containingLClass: LClass): LProperty?

    context(element: PsiElement)
    fun containingClass(): PsiNamedElement?

    context(element: PsiElement)
    fun isAnnotationClass(): Boolean

    context(element: PsiElement)
    fun isEnumClass(): Boolean

    context(element: PsiElement)
    fun classQualifiedName(): String?

    context(element: PsiElement)
    fun qualifiedEnumConstant(): Pair<String, String>?

    context(element: PsiElement)
    fun lAnnotationParams(values: Map<String, LAnnotation.Param.Value?>): List<LAnnotation.Param>?

    /**
     * 接受任意 PsiElement，对每一个都有答案，包括"我不认识你"
     */
    context(element: PsiElement)
    fun kind(): LKind?

    context(element: PsiElement)
    fun hasAnnotation(vararg annotation: ClassId): Boolean

    context(element: PsiElement)
    fun typeArgumentFor(superName: String, index: Int = 0): PsiNamedElement?

    context(element: PsiElement)
    fun topLevelClasses(): List<PsiNamedElement>

    context(element: PsiElement)
    fun builtinAliases(): List<String>

    context(element: PsiElement)
    fun nestedTypes(): List<PsiNamedElement>

    context(element: PsiElement)
    fun enumConstants(): List<PsiNamedElement>

    context(element: PsiElement)
    fun isInheritorOrSelf(qualifiedName: String, baseName: String): Boolean?
}

fun Language.processor(): LanguageProcessor? {
    return EP.findFirstSafe { it.supports(this) }
}

/**
 * @return null：实体不存在，或语言不被支持。后者目前没有处理需求。
 */
inline fun <R> process(element: PsiElement, action: context(PsiElement, ResolvedTypes) LanguageProcessor.() -> R): R? {
    return element.language.processor()?.let { action(element, ResolvedTypes(), it) }
}

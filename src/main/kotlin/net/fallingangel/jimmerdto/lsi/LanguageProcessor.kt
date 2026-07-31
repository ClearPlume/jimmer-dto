package net.fallingangel.jimmerdto.lsi

import com.intellij.lang.Language
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotation
import kotlin.reflect.KClass

private val EP = ExtensionPointName.create<LanguageProcessor>("net.fallingangel.languageProcessor")

interface LanguageProcessor {
    fun supports(language: Language): Boolean

    context(element: PsiElement, types: ResolvedTypes)
    fun lClass(): LClass?

    context(element: PsiElement, types: ResolvedTypes)
    fun lProperty(containingLClass: LClass): LProperty?

    context(element: PsiElement)
    fun isAnnotationClass(): Boolean

    context(element: PsiElement)
    fun classQualifiedName(): String?

    context(element: PsiElement)
    fun enum(): Pair<String, String>?

    context(element: PsiElement)
    fun lAnnotationParams(values: Map<String, LAnnotation.Param.Value?>): List<LAnnotation.Param>?

    context(project: Project)
    fun builtinType(name: String): PsiElement?

    context(element: PsiElement)
    fun kind(): LKind?

    context(element: PsiElement)
    fun hasAnnotation(vararg annotation: KClass<out Annotation>): Boolean
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

inline fun <R> process(language: Language, project: Project, action: context(Project) LanguageProcessor.() -> R): R? {
    return language.processor()?.let { action(project, it) }
}
package net.fallingangel.jimmerdto.lsi

import com.intellij.lang.Language
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.ProjectScope
import net.fallingangel.jimmerdto.exception.UnsupportedLanguageException
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotation
import net.fallingangel.jimmerdto.psi.DTOFile
import net.fallingangel.jimmerdto.psi.element.DTOAnnotationValue
import net.fallingangel.jimmerdto.util.literalType

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
    fun canonicalName(): String?

    context(element: PsiElement)
    fun enum(): Pair<String, String>?

    context(element: PsiElement)
    fun lAnnotationParams(values: Map<String, LAnnotation.Param.Value?>): List<LAnnotation.Param>?

    context(project: Project)
    fun builtinType(name: String): PsiElement?

    fun supports(dtoFile: DTOFile): Boolean

    fun type(value: DTOAnnotationValue): PsiType? {
        val singleValue = value.singleValue ?: return null

        val qualifiedName = singleValue.qualifiedName
        val nestAnnotation = singleValue.nestAnnotation

        val project = value.project
        val scope = ProjectScope.getAllScope(project)
        if (qualifiedName != null) {
            if (singleValue.classSuffix == null) {
                // qualifiedName有可能表示枚举字面量
                // 获取倒第二part，校验qualifiedName是否为枚举
                val enumType = qualifiedName.parts[qualifiedName.parts.size - 2].resolve() as? PsiClass ?: return null
                return if (qualifiedName.parts.size >= 2 && enumType.isEnum) {
                    PsiClassType.getTypeByName(enumType.qualifiedName!!, project, scope)
                } else {
                    PsiType.getTypeByName(qualifiedName.value, project, scope)
                }
            } else {
                val clazz = JavaPsiFacade.getInstance(project).findClass("java.lang.Class", scope) ?: return null
                val typeName = qualifiedName.clazz?.qualifiedName ?: return null
                val classGenericType = PsiType.getTypeByName(typeName, project, scope)
                return PsiElementFactory.getInstance(project).createType(clazz, classGenericType)
            }
        }

        if (nestAnnotation != null) {
            val typeName = nestAnnotation.qualifiedName.clazz?.qualifiedName ?: return null
            return PsiType.getTypeByName(typeName, project, scope)
        }

        return project.literalType(value.text)
    }

    companion object {
        private val extensionPointName = ExtensionPointName.create<LanguageProcessor>("net.fallingangel.languageProcessor")

        fun analyze(dtoFile: DTOFile): LanguageProcessor {
            val processor = extensionPointName.findFirstSafe { it.supports(dtoFile) }
            return processor ?: throw UnsupportedLanguageException("Unsupported language")
        }
    }
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
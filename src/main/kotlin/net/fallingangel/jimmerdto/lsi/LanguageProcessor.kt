package net.fallingangel.jimmerdto.lsi

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.exception.UnsupportedLanguageException
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotation
import net.fallingangel.jimmerdto.psi.DTOFile

/**
 * @param C 类Psi元素类型
 * @param A 注解Psi元素类型
 * @param T 类型元素类型
 */
interface LanguageProcessor<C : PsiElement, A : PsiElement, T> {
    val resolvedType: MutableMap<String, LClass<C>>

    fun clearTypeCache() = resolvedType.clear()

    fun init(project: Project)

    fun supports(dtoFile: DTOFile): Boolean

    fun clazz(dtoFile: DTOFile): LClass<C>

    fun clazz(clazz: C): LClass<C>

    fun parents(clazz: C): List<LClass<C>>

    fun properties(clazz: C): List<LProperty<*>>

    fun methods(clazz: C): List<LMethod<*>>

    fun resolve(type: T): LType

    fun resolve(annotation: A): LAnnotation<*>

    fun annotation(qualifiedName: String): LAnnotation<*>

    fun annotation(clazz: C): LAnnotation<*>

    companion object {
        fun analyze(dtoFile: DTOFile): LanguageProcessor<*, *, *> {
            val extensionPointName = ExtensionPointName.create<LanguageProcessor<*, *, *>>("net.fallingangel.languageProcessor")
            val processor = extensionPointName.findFirstSafe { it.supports(dtoFile) }
            return processor?.apply { init(dtoFile.project) }
                ?: throw UnsupportedLanguageException("Unsupported language: ${dtoFile.projectLanguage}")
        }
    }
}
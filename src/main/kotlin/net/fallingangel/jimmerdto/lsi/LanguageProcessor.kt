package net.fallingangel.jimmerdto.lsi

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.exception.UnsupportedLanguageException
import net.fallingangel.jimmerdto.psi.DTOFile

/**
 * @param C 类Psi元素类型
 */
interface LanguageProcessor<C : PsiElement> {
    val resolvedType: MutableMap<String, LClass<C>>

    fun clearTypeCache() = resolvedType.clear()

    fun init(project: Project)

    fun supports(dtoFile: DTOFile): Boolean

    fun clazz(dtoFile: DTOFile): LClass<C>

    fun clazz(clazz: C): LClass<C>

    fun parents(clazz: C): List<LClass<C>>

    fun properties(clazz: C): List<LProperty<*>>

    fun methods(clazz: C): List<LMethod<*>>

    companion object {
        private val extensionPointName = ExtensionPointName.create<LanguageProcessor<*>>("net.fallingangel.languageProcessor")

        fun analyze(dtoFile: DTOFile): LanguageProcessor<*> {
            val processor = extensionPointName.findFirstSafe { it.supports(dtoFile) }
            return processor?.apply { init(dtoFile.project) }
                ?: throw UnsupportedLanguageException("Unsupported language: ${dtoFile.projectLanguage}")
        }
    }
}
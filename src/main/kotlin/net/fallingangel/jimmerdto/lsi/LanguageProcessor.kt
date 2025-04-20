package net.fallingangel.jimmerdto.lsi

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.psi.*
import com.intellij.psi.search.ProjectScope
import net.fallingangel.jimmerdto.exception.UnsupportedLanguageException
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotationOwner
import net.fallingangel.jimmerdto.psi.DTOFile
import net.fallingangel.jimmerdto.psi.element.DTOAnnotationValue
import net.fallingangel.jimmerdto.util.literalType

/**
 * @param C 类Psi元素类型
 */
interface LanguageProcessor<C : PsiElement> {
    val resolvedType: MutableMap<String, LClass<C>>

    fun clearTypeCache() = resolvedType.clear()

    fun supports(dtoFile: DTOFile): Boolean

    fun clazz(dtoFile: DTOFile): LClass<C>

    fun clazz(clazz: C): LClass<C>

    fun parents(clazz: C): List<LClass<C>>

    fun properties(clazz: C): List<LProperty<*>>

    fun methods(clazz: C): List<LMethod<*>>

    fun resolve(element: PsiElement): LAnnotationOwner?

    /**
     * 数组字面量不以这种方式判定其类型，类型提升过于复杂，性价比不高
     */
    fun type(type: PsiType, value: DTOAnnotationValue): PsiType? {
        value.arrayValue?.let { return PsiArrayType(PsiType.VOID) }
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
            return PsiType.getTypeByName(nestAnnotation.qualifiedName.value, project, scope)
        }

        return project.literalType(value.text)
    }

    companion object {
        private val extensionPointName = ExtensionPointName.create<LanguageProcessor<*>>("net.fallingangel.languageProcessor")

        fun analyze(dtoFile: DTOFile): LanguageProcessor<*> {
            val processor = extensionPointName.findFirstSafe { it.supports(dtoFile) }
            return processor ?: throw UnsupportedLanguageException("Unsupported language: ${dtoFile.projectLanguage}")
        }
    }
}
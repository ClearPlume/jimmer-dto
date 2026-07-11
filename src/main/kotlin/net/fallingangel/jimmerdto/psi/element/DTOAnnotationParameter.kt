package net.fallingangel.jimmerdto.psi.element

import com.intellij.psi.PsiAnnotationMethod
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import net.fallingangel.jimmerdto.lsi.LanguageProcessor
import net.fallingangel.jimmerdto.psi.mixin.DTONamedElement
import net.fallingangel.jimmerdto.util.file

interface DTOAnnotationParameter : DTONamedElement {
    val name: PsiElement

    val eq: PsiElement

    val value: DTOAnnotationValue?

    val sourceElement: PsiAnnotationMethod?
        get() {
            val anno = parent
            val annoClass = if (anno is DTOAnnotation) {
                anno.qualifiedName.clazz
            } else {
                anno as DTONestAnnotation
                anno.qualifiedName.clazz
            }
            annoClass ?: return null
            return annoClass.findMethodsByName(name.text, false).firstOrNull() as? PsiAnnotationMethod
        }

    val type: PsiType?
        get() = sourceElement?.returnType

    val valueType: PsiType?
        get() {
            val processor = LanguageProcessor.analyze(file)
            return value?.let { processor.type(it) }
        }

    val valueAssignableFromType: Boolean
        get() {
            val type = type ?: return false
            val actualType = if (type is PsiArrayType) {
                type.componentType
            } else {
                type
            }
            val valueType = valueType ?: return false
            return actualType.isAssignableFrom(valueType)
        }
}
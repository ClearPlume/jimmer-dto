package net.fallingangel.jimmerdto.psi.element

import com.intellij.psi.PsiAnnotationMethod
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import net.fallingangel.jimmerdto.psi.mixin.DTONamedElement

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
}
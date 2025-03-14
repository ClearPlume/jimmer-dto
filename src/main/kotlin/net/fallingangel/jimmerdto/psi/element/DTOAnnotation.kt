package net.fallingangel.jimmerdto.psi.element

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTOAnnotation : DTOElement {
    val at: PsiElement

    val qualifiedName: DTOQualifiedName

    val value: DTOAnnotationValue?

    val params: List<DTOAnnotationParameter>
}
package net.fallingangel.jimmerdto.psi.element

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTOAnnotationSingleValue : DTOElement {
    val nestAnnotation: DTONestAnnotation?

    val qualifiedName: DTOQualifiedName?

    val classSuffix: PsiElement?
}
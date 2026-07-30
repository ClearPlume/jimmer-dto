package net.fallingangel.jimmerdto.psi.element

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.psi.mixin.DTOAnnotationElement

interface DTONestAnnotation : DTOAnnotationElement {
    val at: PsiElement?
}
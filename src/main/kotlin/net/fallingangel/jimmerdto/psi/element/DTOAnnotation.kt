package net.fallingangel.jimmerdto.psi.element

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.psi.mixin.DTOAnnotationElement

interface DTOAnnotation : DTOAnnotationElement {
    val at: PsiElement
}
package net.fallingangel.jimmerdto.psi.element

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTOAnnotationParameter : DTOElement {
    val name: PsiElement

    val eq: PsiElement

    val value: DTOAnnotationValue
}
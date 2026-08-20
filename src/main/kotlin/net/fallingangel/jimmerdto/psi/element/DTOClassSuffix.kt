package net.fallingangel.jimmerdto.psi.element

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTOClassSuffix : DTOElement {
    val classOperator: PsiElement

    val classToken: PsiElement?

    val unsupportedSuffix: PsiElement?
}

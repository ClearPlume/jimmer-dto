package net.fallingangel.jimmerdto.psi.element

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTOAliasGroup : DTOElement {
    val `as`: PsiElement

    val power: PsiElement?

    val original: PsiElement?

    val dollar: PsiElement?

    val replacement: PsiElement?
}
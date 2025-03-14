package net.fallingangel.jimmerdto.psi.element

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTOPropFlag : DTOElement {
    val insensitive: PsiElement?

    val power: PsiElement?

    val dollar: PsiElement?
}
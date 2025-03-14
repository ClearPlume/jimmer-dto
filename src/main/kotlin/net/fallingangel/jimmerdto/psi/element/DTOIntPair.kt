package net.fallingangel.jimmerdto.psi.element

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTOIntPair : DTOElement {
    val first: PsiElement

    val second: PsiElement?
}
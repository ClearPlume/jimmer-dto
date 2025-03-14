package net.fallingangel.jimmerdto.psi.element

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTOPropValue : DTOElement {
    val boolean: PsiElement?

    val character: PsiElement?

    val sqlString: PsiElement?

    val integer: PsiElement?

    val float: PsiElement?
}
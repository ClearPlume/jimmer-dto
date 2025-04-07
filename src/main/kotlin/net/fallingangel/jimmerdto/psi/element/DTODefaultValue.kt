package net.fallingangel.jimmerdto.psi.element

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTODefaultValue : DTOElement {
    val boolean: PsiElement?

    val integer: PsiElement?

    val string: PsiElement?

    val float: PsiElement?

    val `null`: PsiElement?
}
package net.fallingangel.jimmerdto.psi.element

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTONullity : DTOElement {
    val prop: DTOQualifiedName

    val `is`: PsiElement

    val not: PsiElement?

    val `null`: PsiElement
}
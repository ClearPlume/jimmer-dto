package net.fallingangel.jimmerdto.psi.element

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTOOrderItem : DTOElement {
    val prop: DTOQualifiedName

    val asc: PsiElement?

    val desc: PsiElement?
}
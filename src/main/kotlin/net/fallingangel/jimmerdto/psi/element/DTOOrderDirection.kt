package net.fallingangel.jimmerdto.psi.element

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTOOrderDirection : DTOElement {
    val asc: PsiElement?

    val desc: PsiElement?

    val identifier: PsiElement?
}
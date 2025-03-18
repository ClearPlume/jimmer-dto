package net.fallingangel.jimmerdto.psi.element

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTOGenericArgument : DTOElement {
    val star: PsiElement?

    val modifier: PsiElement?

    val type: DTOTypeRef?
}
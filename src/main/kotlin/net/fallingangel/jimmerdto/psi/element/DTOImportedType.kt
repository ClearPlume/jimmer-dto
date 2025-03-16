package net.fallingangel.jimmerdto.psi.element

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTOImportedType : DTOElement {
    val type: PsiElement

    val alias: PsiElement?
}
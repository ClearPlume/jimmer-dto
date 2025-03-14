package net.fallingangel.jimmerdto.psi.element

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTOMacroArgs : DTOElement {
    val l: PsiElement

    val values: List<DTOQualifiedName>

    val r: PsiElement
}
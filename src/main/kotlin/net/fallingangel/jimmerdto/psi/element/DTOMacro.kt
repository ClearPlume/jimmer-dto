package net.fallingangel.jimmerdto.psi.element

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTOMacro : DTOElement {
    val hash: PsiElement

    val name: DTOMacroName

    val args: DTOMacroArgs?
}
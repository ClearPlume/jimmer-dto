package net.fallingangel.jimmerdto.psi.element

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.psi.mixin.DTONamedElement

interface DTOEnumMappingConstant : DTONamedElement {
    val constant: PsiElement
}
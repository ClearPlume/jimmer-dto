package net.fallingangel.jimmerdto.psi.element

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTOEnumMapping : DTOElement {
    val constant: DTOEnumMappingConstant

    val string: PsiElement?

    val int: PsiElement?
}
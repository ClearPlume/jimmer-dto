package net.fallingangel.jimmerdto.psi.element

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTOClassKeyword : DTOElement {
    val classToken: PsiElement?

    val unsupportedKeyword: PsiElement?
}

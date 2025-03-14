package net.fallingangel.jimmerdto.psi.element

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTOCompareSymbol : DTOElement {
    val equals: PsiElement?

    val notEquals1: PsiElement?

    val notEquals2: PsiElement?

    val lessThan: PsiElement?

    val lessThanEquals: PsiElement?

    val greaterThan: PsiElement?

    val greaterThanEquals: PsiElement?

    val like: PsiElement?

    val ilike: PsiElement?
}
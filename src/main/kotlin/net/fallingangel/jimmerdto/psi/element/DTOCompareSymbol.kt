package net.fallingangel.jimmerdto.psi.element

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.enums.SimplePropType
import net.fallingangel.jimmerdto.psi.grammarMismatch
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

    val kind: Kind
        get() = when {
            equals != null -> Kind.Equals
            notEquals1 != null || notEquals2 != null -> Kind.NotEquals
            lessThan != null -> Kind.LessThan
            lessThanEquals != null -> Kind.LessThanEquals
            greaterThan != null -> Kind.GreaterThan
            greaterThanEquals != null -> Kind.GreaterThanEquals
            like != null -> Kind.Like
            ilike != null -> Kind.ILike
            else -> grammarMismatch()
        }

    enum class Kind(val requires: SimplePropType.Family? = null) {
        Equals, NotEquals, LessThan, LessThanEquals, GreaterThan, GreaterThanEquals,
        Like(SimplePropType.Family.String), ILike(SimplePropType.Family.String),
    }
}
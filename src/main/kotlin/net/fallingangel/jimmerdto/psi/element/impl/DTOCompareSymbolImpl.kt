package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.DTOCompareSymbol
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.util.findChildNullable
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTOCompareSymbolImpl(node: ASTNode) : ANTLRPsiNode(node), DTOCompareSymbol {
    override val equals: PsiElement?
        get() = findChildNullable("/compareSymbol/Equals")

    override val notEquals1: PsiElement?
        get() = findChildNullable("/compareSymbol/NotEquals1")

    override val notEquals2: PsiElement?
        get() = findChildNullable("/compareSymbol/NotEquals2")

    override val lessThan: PsiElement?
        get() = findChildNullable("/compareSymbol/LessThan")

    override val lessThanEquals: PsiElement?
        get() = findChildNullable("/compareSymbol/LessThanEquals")

    override val greaterThan: PsiElement?
        get() = findChildNullable("/compareSymbol/GreaterThan")

    override val greaterThanEquals: PsiElement?
        get() = findChildNullable("/compareSymbol/GreaterThanEquals")

    override val like: PsiElement?
        get() = findChildNullable("/compareSymbol/Like")

    override val ilike: PsiElement?
        get() = findChildNullable("/compareSymbol/Ilike")

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitCompareSymbol(this)
        } else {
            super.accept(visitor)
        }
    }
}
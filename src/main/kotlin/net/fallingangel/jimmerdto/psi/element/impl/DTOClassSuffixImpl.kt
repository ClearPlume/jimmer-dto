package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.DTOClassSuffix
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.psi.grammarMismatch
import net.fallingangel.jimmerdto.util.findChildNullable
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTOClassSuffixImpl(node: ASTNode) : ANTLRPsiNode(node), DTOClassSuffix {
    override val classOperator: PsiElement
        get() = findChildNullable("/classSuffix/Dot") ?: findChildNullable("/classSuffix/DoubleColon") ?: grammarMismatch()
    override val classToken: PsiElement?
        get() = findChildNullable("/classSuffix/Class")

    override val unsupportedSuffix: PsiElement?
        get() = findChildNullable("/classSuffix/Identifier")

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitClassSuffix(this)
        } else {
            super.accept(visitor)
        }
    }
}

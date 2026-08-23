package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.DTOClassKeyword
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.util.findChildNullable
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTOClassKeywordImpl(node: ASTNode) : ANTLRPsiNode(node), DTOClassKeyword {
    override val classToken: PsiElement?
        get() = findChildNullable("/classKeyword/Class")

    override val unsupportedKeyword: PsiElement?
        get() = findChildNullable("/classKeyword/Identifier")

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitClassKeyword(this)
        } else {
            super.accept(visitor)
        }
    }
}

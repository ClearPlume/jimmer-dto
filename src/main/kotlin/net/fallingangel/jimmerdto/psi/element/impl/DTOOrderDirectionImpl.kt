package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.DTOOrderDirection
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.util.findChildNullable
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTOOrderDirectionImpl(node: ASTNode) : ANTLRPsiNode(node), DTOOrderDirection {
    override val asc: PsiElement?
        get() = findChildNullable("/orderDirection/Asc")

    override val desc: PsiElement?
        get() = findChildNullable("/orderDirection/Desc")

    override val identifier: PsiElement?
        get() = findChildNullable("/orderDirection/Identifier")

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitOrderDirection(this)
        } else {
            super.accept(visitor)
        }
    }
}
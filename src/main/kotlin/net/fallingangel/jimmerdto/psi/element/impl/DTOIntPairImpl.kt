package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.DTOIntPair
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.util.findChild
import net.fallingangel.jimmerdto.util.findChildren
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTOIntPairImpl(node: ASTNode) : ANTLRPsiNode(node), DTOIntPair {
    override val first: PsiElement
        get() = findChild("/intPair/IntegerLiteral")

    override val second: PsiElement?
        get() = findChildren<PsiElement>("/intPair/IntegerLiteral").getOrNull(1)

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitIntPair(this)
        } else {
            super.accept(visitor)
        }
    }
}
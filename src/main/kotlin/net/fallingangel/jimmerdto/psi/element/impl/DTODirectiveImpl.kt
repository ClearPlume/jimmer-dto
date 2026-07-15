package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.DTODirective
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTODirectiveImpl(node: ASTNode) : ANTLRPsiNode(node), DTODirective {
    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitDirective(this)
        } else {
            super.accept(visitor)
        }
    }
}
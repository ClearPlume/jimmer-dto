package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.DTOValue
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTOValueImpl(node: ASTNode) : ANTLRPsiNode(node), DTOValue {
    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitValue(this)
        } else {
            super.accept(visitor)
        }
    }
}

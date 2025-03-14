package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.DTOOrderByArgs
import net.fallingangel.jimmerdto.psi.element.DTOOrderItem
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.util.findChildren
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTOOrderByArgsImpl(node: ASTNode) : ANTLRPsiNode(node), DTOOrderByArgs {
    override val orderItems: List<DTOOrderItem>
        get() = findChildren("/orderByArgs/orderItem")

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitOrderByArgs(this)
        } else {
            super.accept(visitor)
        }
    }
}
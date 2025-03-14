package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.DTOPropArg
import net.fallingangel.jimmerdto.psi.element.DTOValue
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.util.findChildren
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTOPropArgImpl(node: ASTNode) : ANTLRPsiNode(node), DTOPropArg {
    override val values: List<DTOValue>
        get() = findChildren("/propArg/value")

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitPropArg(this)
        } else {
            super.accept(visitor)
        }
    }
}

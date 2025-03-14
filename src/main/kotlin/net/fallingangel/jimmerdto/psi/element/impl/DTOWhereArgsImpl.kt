package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.DTOPredicate
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.psi.element.DTOWhereArgs
import net.fallingangel.jimmerdto.util.findChildren
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTOWhereArgsImpl(node: ASTNode) : ANTLRPsiNode(node), DTOWhereArgs {
    override val predicates: List<DTOPredicate>
        get() = findChildren("/whereArgs/predicate")

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitWhereArgs(this)
        } else {
            super.accept(visitor)
        }
    }
}
package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.DTOCompare
import net.fallingangel.jimmerdto.psi.element.DTONullity
import net.fallingangel.jimmerdto.psi.element.DTOPredicate
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.util.findChildNullable
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTOPredicateImpl(node: ASTNode) : ANTLRPsiNode(node), DTOPredicate {
    override val compare: DTOCompare?
        get() = findChildNullable("/predicate/compare")

    override val nullity: DTONullity?
        get() = findChildNullable("/predicate/nullity")

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitPredicate(this)
        } else {
            super.accept(visitor)
        }
    }
}
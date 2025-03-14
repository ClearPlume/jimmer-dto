package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.DTOPropFlag
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.util.findChildNullable
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTOPropFlagImpl(node: ASTNode) : ANTLRPsiNode(node), DTOPropFlag {
    override val insensitive: PsiElement?
        get() = findChildNullable("/propFlag/'i'")

    override val power: PsiElement?
        get() = findChildNullable("/propFlag/'^'")

    override val dollar: PsiElement?
        get() = findChildNullable("/propFlag/'$'")

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitPropFlag(this)
        } else {
            super.accept(visitor)
        }
    }
}

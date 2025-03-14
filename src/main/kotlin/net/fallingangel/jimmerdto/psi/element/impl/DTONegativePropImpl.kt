package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.DTONegativeProp
import net.fallingangel.jimmerdto.psi.element.DTOPropName
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.util.findChildNullable
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTONegativePropImpl(node: ASTNode) : ANTLRPsiNode(node), DTONegativeProp {
    override val name: DTOPropName?
        get() = findChildNullable("/negativeProp/propName")

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitNegativeProp(this)
        } else {
            super.accept(visitor)
        }
    }
}

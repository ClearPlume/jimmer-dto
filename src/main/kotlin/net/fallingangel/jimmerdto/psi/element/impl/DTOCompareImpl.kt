package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.*
import net.fallingangel.jimmerdto.util.findChild
import net.fallingangel.jimmerdto.util.findChildNullable
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTOCompareImpl(node: ASTNode) : ANTLRPsiNode(node), DTOCompare {
    override val prop: DTOQualifiedName
        get() = findChild("/compare/qualifiedName")

    override val symbol: DTOCompareSymbol?
        get() = findChildNullable("/compare/compareSymbol")

    override val value: DTOPropValue?
        get() = findChildNullable("/compare/propValue")

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitCompare(this)
        } else {
            super.accept(visitor)
        }
    }
}
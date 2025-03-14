package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.DTOEnumMapping
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.util.findChild
import net.fallingangel.jimmerdto.util.findChildNullable
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTOEnumMappingImpl(node: ASTNode) : ANTLRPsiNode(node), DTOEnumMapping {
    override val constant: PsiElement
        get() = findChild("/enumMapping/Identifier")

    override val string: PsiElement?
        get() = findChildNullable("/enumMapping/StringLiteral")

    override val int: PsiElement?
        get() = findChildNullable("/enumMapping/IntegerLiteral")

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitEnumMapping(this)
        } else {
            super.accept(visitor)
        }
    }
}

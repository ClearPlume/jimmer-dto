package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.DTOPropName
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.util.findChild
import net.fallingangel.jimmerdto.util.findChildNullable
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTOPropNameImpl(node: ASTNode) : ANTLRPsiNode(node), DTOPropName {
    override val value: String
        get() {
            val quoted = findChildNullable<PsiElement>("/propName/QuotedSymbol")
            if (quoted != null) {
                val text = quoted.text
                return text.substring(1, text.length - 1)
            }
            return findChild<PsiElement>("/propName/Identifier").text
        }

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitPropName(this)
        } else {
            super.accept(visitor)
        }
    }
}

package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.elementType
import net.fallingangel.jimmerdto.psi.DTOParser
import net.fallingangel.jimmerdto.psi.element.DTOExportStatement
import net.fallingangel.jimmerdto.psi.element.DTOQualifiedName
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.util.findChild
import net.fallingangel.jimmerdto.util.findChildNullable
import net.fallingangel.jimmerdto.util.sibling
import org.antlr.intellij.adaptor.lexer.RuleIElementType
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTOExportStatementImpl(node: ASTNode) : ANTLRPsiNode(node), DTOExportStatement {
    override val export: DTOQualifiedName
        get() = findChild("/exportStatement/qualifiedName")

    override val `package`: DTOQualifiedName?
        get() = findChildNullable<PsiElement>("/exportStatement/Package")
                ?.sibling {
                    val type = it.elementType
                    type is RuleIElementType && type.ruleIndex == DTOParser.RULE_qualifiedName
                }

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitExportStatement(this)
        } else {
            super.accept(visitor)
        }
    }
}

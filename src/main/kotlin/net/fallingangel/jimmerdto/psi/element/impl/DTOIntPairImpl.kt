package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.elementType
import net.fallingangel.jimmerdto.psi.DTOLexer
import net.fallingangel.jimmerdto.psi.element.DTOIntPair
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.util.findChild
import net.fallingangel.jimmerdto.util.findChildNullable
import net.fallingangel.jimmerdto.util.sibling
import org.antlr.intellij.adaptor.lexer.TokenIElementType
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTOIntPairImpl(node: ASTNode) : ANTLRPsiNode(node), DTOIntPair {
    override val first: PsiElement
        get() = findChild("/intPair/IntegerLiteral")

    override val second: PsiElement?
        get() = findChildNullable<PsiElement>("/intPair/','")
                ?.sibling {
                    val type = it.elementType
                    type is TokenIElementType && type.antlrTokenType == DTOLexer.IntegerLiteral
                }

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitIntPair(this)
        } else {
            super.accept(visitor)
        }
    }
}
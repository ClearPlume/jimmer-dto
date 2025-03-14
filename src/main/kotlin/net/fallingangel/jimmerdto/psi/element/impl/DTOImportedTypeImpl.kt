package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.elementType
import net.fallingangel.jimmerdto.psi.DTOLexer
import net.fallingangel.jimmerdto.psi.element.DTOImportedType
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.util.findChild
import net.fallingangel.jimmerdto.util.findChildNullable
import net.fallingangel.jimmerdto.util.sibling
import org.antlr.intellij.adaptor.lexer.TokenIElementType
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTOImportedTypeImpl(node: ASTNode) : ANTLRPsiNode(node), DTOImportedType {
    override val type: String
        get() = findChild<PsiElement>("/importedType/Identifier").text

    override val alias: String?
        get() = findChildNullable<PsiElement>("/importedType/As")
                ?.sibling<PsiElement> {
                    val type = it.elementType
                    type is TokenIElementType && type.antlrTokenType == DTOLexer.Identifier
                }
                ?.text

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitImportedType(this)
        } else {
            super.accept(visitor)
        }
    }
}

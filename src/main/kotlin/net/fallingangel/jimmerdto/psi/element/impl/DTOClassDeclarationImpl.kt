package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.DTOClassDeclaration
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.util.findChildNullable
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTOClassDeclarationImpl(node: ASTNode) : ANTLRPsiNode(node), DTOClassDeclaration {
    override val name: PsiElement?
        get() = findChildNullable("/classDeclaration/Identifier")

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitClassDeclaration(this)
        } else {
            super.accept(visitor)
        }
    }
}
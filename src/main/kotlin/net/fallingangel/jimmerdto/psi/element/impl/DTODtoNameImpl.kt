package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.DTODtoName
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.util.findChild
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTODtoNameImpl(node: ASTNode) : ANTLRPsiNode(node), DTODtoName {
    override val value: String
        get() = findChild<PsiElement>("/dtoName/Identifier").text

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitDtoName(this)
        } else {
            super.accept(visitor)
        }
    }
}

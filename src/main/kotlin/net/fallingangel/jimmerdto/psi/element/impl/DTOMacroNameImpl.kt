package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.DTOMacroName
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.util.findChild
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTOMacroNameImpl(node: ASTNode) : ANTLRPsiNode(node), DTOMacroName {
    override val value: String
        get() = findChild<PsiElement>("/macroName/Identifier").text

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitMacroName(this)
        } else {
            super.accept(visitor)
        }
    }
}

package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.DTOMacro
import net.fallingangel.jimmerdto.psi.element.DTOMacroArgs
import net.fallingangel.jimmerdto.psi.element.DTOMacroName
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.util.findChild
import net.fallingangel.jimmerdto.util.findChildNullable
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTOMacroImpl(node: ASTNode) : ANTLRPsiNode(node), DTOMacro {
    override val hash: PsiElement
        get() = findChild<PsiElement>("/macro/'#'")

    override val name: DTOMacroName
        get() = findChild("/macro/macroName")

    override val args: DTOMacroArgs?
        get() = findChildNullable("/macro/macroArgs")

    override val optional: PsiElement?
        get() = findChildNullable("/macro/'?'")

    override val required: PsiElement?
        get() = findChildNullable("/macro/'!'")

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitMacro(this)
        } else {
            super.accept(visitor)
        }
    }
}

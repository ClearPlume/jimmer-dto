package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.DTOMacroArgs
import net.fallingangel.jimmerdto.psi.element.DTOQualifiedName
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.util.findChild
import net.fallingangel.jimmerdto.util.findChildren
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTOMacroArgsImpl(node: ASTNode) : ANTLRPsiNode(node), DTOMacroArgs {
    override val l: PsiElement
        get() = findChild("/macroArgs/LParen")

    override val values: List<DTOQualifiedName>
        get() = findChildren("/macroArgs/qualifiedName")

    override val r: PsiElement
        get() = findChild("/macroArgs/RParen")

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitMacroArgs(this)
        } else {
            super.accept(visitor)
        }
    }
}

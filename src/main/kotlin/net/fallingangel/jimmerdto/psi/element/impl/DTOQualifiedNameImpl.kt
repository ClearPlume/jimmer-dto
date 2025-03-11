package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.DTOQualifiedName
import net.fallingangel.jimmerdto.psi.element.DTOQualifiedNamePart
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.util.findChildren
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTOQualifiedNameImpl(node: ASTNode) : ANTLRPsiNode(node), DTOQualifiedName {
    override val parts: List<DTOQualifiedNamePart>
        get() = findChildren("/qualifiedName/qualifiedNamePart")

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitQualifiedName(this)
        } else {
            super.accept(visitor)
        }
    }
}

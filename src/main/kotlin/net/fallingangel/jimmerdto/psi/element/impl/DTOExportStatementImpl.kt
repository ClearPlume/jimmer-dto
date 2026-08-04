package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.DTOExportStatement
import net.fallingangel.jimmerdto.psi.element.DTOQualifiedName
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.util.findChild
import net.fallingangel.jimmerdto.util.findChildren
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTOExportStatementImpl(node: ASTNode) : ANTLRPsiNode(node), DTOExportStatement {
    override val export: DTOQualifiedName
        get() = findChild("/exportStatement/qualifiedName")

    override val `package`: DTOQualifiedName?
        get() = findChildren<DTOQualifiedName>("/exportStatement/qualifiedName").getOrNull(1)

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitExportStatement(this)
        } else {
            super.accept(visitor)
        }
    }
}

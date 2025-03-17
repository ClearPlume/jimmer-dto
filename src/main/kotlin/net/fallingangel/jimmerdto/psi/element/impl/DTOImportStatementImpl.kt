package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.*
import net.fallingangel.jimmerdto.util.findChild
import net.fallingangel.jimmerdto.util.findChildNullable
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTOImportStatementImpl(node: ASTNode) : ANTLRPsiNode(node), DTOImportStatement {
    override val qualifiedName: DTOQualifiedName
        get() = findChild("/importStatement/qualifiedName")

    override val alias: DTOAlias?
        get() = findChildNullable("/importStatement/alias")

    override val groupedImport: DTOGroupedImport?
        get() = findChildNullable("/importStatement/groupedImport")

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitImportStatement(this)
        } else {
            super.accept(visitor)
        }
    }
}

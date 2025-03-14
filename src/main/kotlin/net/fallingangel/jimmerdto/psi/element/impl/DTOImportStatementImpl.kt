package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.DTOImportStatement
import net.fallingangel.jimmerdto.psi.element.DTOImportedType
import net.fallingangel.jimmerdto.psi.element.DTOQualifiedName
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.util.findChild
import net.fallingangel.jimmerdto.util.findChildNullable
import net.fallingangel.jimmerdto.util.findChildren
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTOImportStatementImpl(node: ASTNode) : ANTLRPsiNode(node), DTOImportStatement {
    override val qualifiedName: DTOQualifiedName
        get() = findChild("/importStatement/qualifiedName")

    override val alias: String?
        get() = findChildNullable<PsiElement>("/importStatement/Identifier")?.text

    override val subTypes: List<DTOImportedType>
        get() = findChildren("/importStatement/importedType")

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitImportStatement(this)
        } else {
            super.accept(visitor)
        }
    }
}

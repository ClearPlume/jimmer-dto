package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.DTOAlias
import net.fallingangel.jimmerdto.psi.element.DTOImported
import net.fallingangel.jimmerdto.psi.element.DTOImportedType
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.util.findChild
import net.fallingangel.jimmerdto.util.findChildNullable
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTOImportedTypeImpl(node: ASTNode) : ANTLRPsiNode(node), DTOImportedType {
    override val type: DTOImported
        get() = findChild("/importedType/imported")

    override val alias: DTOAlias?
        get() = findChildNullable("/importedType/alias")

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitImportedType(this)
        } else {
            super.accept(visitor)
        }
    }
}

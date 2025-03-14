package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.DTOEnumBody
import net.fallingangel.jimmerdto.psi.element.DTOEnumMapping
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.util.findChildren
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTOEnumBodyImpl(node: ASTNode) : ANTLRPsiNode(node), DTOEnumBody {
    override val mappings: List<DTOEnumMapping>
        get() = findChildren("/enumBody/enumMapping")

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitEnumBody(this)
        } else {
            super.accept(visitor)
        }
    }
}

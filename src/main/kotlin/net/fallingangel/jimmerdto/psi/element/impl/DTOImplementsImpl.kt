package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.DTOImplements
import net.fallingangel.jimmerdto.psi.element.DTOTypeRef
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.util.findChildren
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTOImplementsImpl(node: ASTNode) : ANTLRPsiNode(node), DTOImplements {
    override val implements: List<DTOTypeRef>
        get() = findChildren("/implements/typeRef")

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitImplements(this)
        } else {
            super.accept(visitor)
        }
    }
}

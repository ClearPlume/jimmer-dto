package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.DTOGenericArgument
import net.fallingangel.jimmerdto.psi.element.DTOGenericArguments
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.util.findChildren
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTOGenericArgumentsImpl(node: ASTNode) : ANTLRPsiNode(node), DTOGenericArguments {
    override val values: List<DTOGenericArgument>
        get() = findChildren("/genericArguments/genericArgument")

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitGenericArguments(this)
        } else {
            super.accept(visitor)
        }
    }
}

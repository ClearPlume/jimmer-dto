package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.*
import net.fallingangel.jimmerdto.util.findChild
import net.fallingangel.jimmerdto.util.findChildren
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTOPolymorphicImpl(node: ASTNode) : ANTLRPsiNode(node), DTOPolymorphic {
    override val directive: DTODirective
        get() = findChild("/polymorphic/directive")

    override val macros: List<DTOMacro>
        get() = findChildren("/polymorphic/macro")

    override val morphisms: List<DTOMorphism>
        get() = children.filterIsInstance<DTOMorphism>()

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitPolymorphic(this)
        } else {
            super.accept(visitor)
        }
    }
}

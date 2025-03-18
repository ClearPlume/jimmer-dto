package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.*
import net.fallingangel.jimmerdto.util.findChild
import net.fallingangel.jimmerdto.util.findChildren
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTOUserPropImpl(node: ASTNode) : ANTLRPsiNode(node), DTOUserProp {
    override val annotations: List<DTOAnnotation>
        get() = findChildren("/userProp/annotation")

    override val name: DTOPropName
        get() = findChild("/userProp/propName")

    override val type: DTOTypeRef
        get() = findChild("/userProp/typeRef")

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitUserProp(this)
        } else {
            super.accept(visitor)
        }
    }
}

package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.DTOAnnotationArrayValue
import net.fallingangel.jimmerdto.psi.element.DTOAnnotationValue
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.util.findChild
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTOAnnotationArrayValueImpl(node: ASTNode) : ANTLRPsiNode(node), DTOAnnotationArrayValue {
    override val values: List<DTOAnnotationValue>
        get() = findChild("/annotationArrayValue/annotationValue")

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitAnnotationArrayValue(this)
        } else {
            super.accept(visitor)
        }
    }
}

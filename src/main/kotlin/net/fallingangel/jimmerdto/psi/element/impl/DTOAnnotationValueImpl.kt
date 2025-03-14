package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.DTOAnnotationArrayValue
import net.fallingangel.jimmerdto.psi.element.DTOAnnotationSingleValue
import net.fallingangel.jimmerdto.psi.element.DTOAnnotationValue
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.util.findChildNullable
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTOAnnotationValueImpl(node: ASTNode) : ANTLRPsiNode(node), DTOAnnotationValue {
    override val singleValue: DTOAnnotationSingleValue?
        get() = findChildNullable("/annotationValue/annotationSingleValue")

    override val arrayValue: DTOAnnotationArrayValue?
        get() = findChildNullable("/annotationValue/annotationArrayValue")

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitAnnotationValue(this)
        } else {
            super.accept(visitor)
        }
    }
}

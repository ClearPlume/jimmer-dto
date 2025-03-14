package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.DTOAnnotationParameter
import net.fallingangel.jimmerdto.psi.element.DTOAnnotationValue
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.util.findChild
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTOAnnotationParameterImpl(node: ASTNode) : ANTLRPsiNode(node), DTOAnnotationParameter {
    override val name: PsiElement
        get() = findChild("/annotationParameter/Identifier")

    override val eq: PsiElement
        get() = findChild("/annotationParameter/'='")

    override val value: DTOAnnotationValue
        get() = findChild("/annotationParameter/annotationValue")

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitAnnotationParameter(this)
        } else {
            super.accept(visitor)
        }
    }
}

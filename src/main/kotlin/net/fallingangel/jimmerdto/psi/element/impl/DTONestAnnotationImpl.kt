package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.*
import net.fallingangel.jimmerdto.util.findChild
import net.fallingangel.jimmerdto.util.findChildNullable
import net.fallingangel.jimmerdto.util.findChildren
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTONestAnnotationImpl(node: ASTNode) : ANTLRPsiNode(node), DTONestAnnotation {
    override val at: PsiElement?
        get() = findChildNullable("/nestedAnnotation/'@'")

    override val qualifiedName: DTOQualifiedName
        get() = findChild("/nestedAnnotation/qualifiedName")

    override val value: DTOAnnotationValue?
        get() = findChildNullable("/nestedAnnotation/annotationValue")

    override val params: List<DTOAnnotationParameter>
        get() = findChildren("/nestedAnnotation/annotationParameter")

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitNestAnnotation(this)
        } else {
            super.accept(visitor)
        }
    }
}

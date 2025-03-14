package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.*
import net.fallingangel.jimmerdto.util.findChild
import net.fallingangel.jimmerdto.util.findChildNullable
import net.fallingangel.jimmerdto.util.findChildren
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTOAnnotationImpl(node: ASTNode) : ANTLRPsiNode(node), DTOAnnotation {
    override val at: PsiElement
        get() = findChild("/annotation/'@'")

    override val qualifiedName: DTOQualifiedName
        get() = findChild("/annotation/qualifiedName")

    override val value: DTOAnnotationValue?
        get() = findChildNullable("/annotation/annotationValue")

    override val params: List<DTOAnnotationParameter>
        get() = findChildren("/annotation/annotationParameter")

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitAnnotation(this)
        } else {
            super.accept(visitor)
        }
    }
}

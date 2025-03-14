package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.*
import net.fallingangel.jimmerdto.util.findChildNullable
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTOAnnotationSingleValueImpl(node: ASTNode) : ANTLRPsiNode(node), DTOAnnotationSingleValue {
    override val annotation: DTOAnnotation?
        get() = findChildNullable("/annotationSingleValue/annotation")

    override val nestAnnotation: DTONestAnnotation?
        get() = findChildNullable("/annotationSingleValue/nestedAnnotation")

    override val qualifiedName: DTOQualifiedName?
        get() = findChildNullable("/annotationSingleValue/qualifiedName")

    override val classSuffix: PsiElement?
        get() = findChildNullable("/annotationSingleValue/classSuffix")

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitAnnotationSingleValue(this)
        } else {
            super.accept(visitor)
        }
    }
}

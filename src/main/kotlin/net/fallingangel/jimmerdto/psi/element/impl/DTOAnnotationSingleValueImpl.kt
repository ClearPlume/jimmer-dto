package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.DTOAnnotationSingleValue
import net.fallingangel.jimmerdto.psi.element.DTONestAnnotation
import net.fallingangel.jimmerdto.psi.element.DTOQualifiedName
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.util.findChildNullable
import net.fallingangel.jimmerdto.util.findChildren
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTOAnnotationSingleValueImpl(node: ASTNode) : ANTLRPsiNode(node), DTOAnnotationSingleValue {
    override val boolean: PsiElement?
        get() = findChildNullable("/annotationSingleValue/BooleanLiteral")

    override val character: PsiElement?
        get() = findChildNullable("/annotationSingleValue/CharacterLiteral")

    override val string: List<PsiElement>
        get() = findChildren("/annotationSingleValue/StringLiteral")

    override val integer: PsiElement?
        get() = findChildNullable("/annotationSingleValue/IntegerLiteral")

    override val float: PsiElement?
        get() = findChildNullable("/annotationSingleValue/FloatingPointLiteral")

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

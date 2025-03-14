package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.DTOPropValue
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.util.findChildNullable
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTOPropValueImpl(node: ASTNode) : ANTLRPsiNode(node), DTOPropValue {
    override val boolean: PsiElement?
        get() = findChildNullable("/propValue/BooleanLiteral")

    override val character: PsiElement?
        get() = findChildNullable("/propValue/CharacterLiteral")

    override val sqlString: PsiElement?
        get() = findChildNullable("/propValue/SqlStringLiteral")

    override val integer: PsiElement?
        get() = findChildNullable("/propValue/IntegerLiteral")

    override val float: PsiElement?
        get() = findChildNullable("/propValue/FloatingPointLiteral")

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitPropValue(this)
        } else {
            super.accept(visitor)
        }
    }
}
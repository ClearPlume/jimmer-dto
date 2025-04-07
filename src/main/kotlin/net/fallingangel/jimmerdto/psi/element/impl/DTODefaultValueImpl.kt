package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.DTODefaultValue
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.util.findChildNullable
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTODefaultValueImpl(node: ASTNode) : ANTLRPsiNode(node), DTODefaultValue {
    override val boolean: PsiElement?
        get() = findChildNullable("/defaultValue/BooleanLiteral")

    override val integer: PsiElement?
        get() = findChildNullable("/defaultValue/IntegerLiteral")

    override val string: PsiElement?
        get() = findChildNullable("/defaultValue/StringLiteral")

    override val float: PsiElement?
        get() = findChildNullable("/defaultValue/FloatingPointLiteral")

    override val `null`: PsiElement?
        get() = findChildNullable("/defaultValue/Null")

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitDefaultValue(this)
        } else {
            super.accept(visitor)
        }
    }
}
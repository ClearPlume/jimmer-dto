package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.DTONullity
import net.fallingangel.jimmerdto.psi.element.DTOQualifiedName
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.util.findChild
import net.fallingangel.jimmerdto.util.findChildNullable
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTONullityImpl(node: ASTNode) : ANTLRPsiNode(node), DTONullity {
    override val prop: DTOQualifiedName
        get() = findChild("/nullity/qualifiedName")

    override val `is`: PsiElement
        get() = findChild("/nullity/Is")

    override val not: PsiElement?
        get() = findChildNullable("/nullity/Not")

    override val `null`: PsiElement
        get() = findChild("/nullity/Null")

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitNullity(this)
        } else {
            super.accept(visitor)
        }
    }
}
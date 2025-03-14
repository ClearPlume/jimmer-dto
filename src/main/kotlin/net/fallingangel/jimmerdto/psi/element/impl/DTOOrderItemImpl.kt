package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.DTOOrderItem
import net.fallingangel.jimmerdto.psi.element.DTOQualifiedName
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.util.findChild
import net.fallingangel.jimmerdto.util.findChildNullable
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTOOrderItemImpl(node: ASTNode) : ANTLRPsiNode(node), DTOOrderItem {
    override val prop: DTOQualifiedName
        get() = findChild("/orderItem/qualifiedName")

    override val asc: PsiElement?
        get() = findChildNullable("/orderItem/Asc")

    override val desc: PsiElement?
        get() = findChildNullable("/orderItem/Desc")

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitOrderItem(this)
        } else {
            super.accept(visitor)
        }
    }
}
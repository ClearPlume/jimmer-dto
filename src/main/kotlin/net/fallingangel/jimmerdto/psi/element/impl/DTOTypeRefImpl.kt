package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.DTOGenericArguments
import net.fallingangel.jimmerdto.psi.element.DTOQualifiedName
import net.fallingangel.jimmerdto.psi.element.DTOTypeRef
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.util.findChild
import net.fallingangel.jimmerdto.util.findChildNullable
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTOTypeRefImpl(node: ASTNode) : ANTLRPsiNode(node), DTOTypeRef {
    override val type: DTOQualifiedName
        get() = findChild("/typeRef/qualifiedName")

    override val arguments: DTOGenericArguments?
        get() = findChildNullable("/typeRef/genericArguments")

    override val questionMark: PsiElement?
        get() = findChildNullable("/typeRef/'?'")

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitTypeDef(this)
        } else {
            super.accept(visitor)
        }
    }
}

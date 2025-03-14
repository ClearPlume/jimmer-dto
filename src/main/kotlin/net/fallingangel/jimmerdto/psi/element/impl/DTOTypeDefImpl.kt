package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.DTOGenericArgument
import net.fallingangel.jimmerdto.psi.element.DTOQualifiedName
import net.fallingangel.jimmerdto.psi.element.DTOTypeDef
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.util.findChild
import net.fallingangel.jimmerdto.util.findChildNullable
import net.fallingangel.jimmerdto.util.findChildren
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTOTypeDefImpl(node: ASTNode) : ANTLRPsiNode(node), DTOTypeDef {
    override val type: DTOQualifiedName
        get() = findChild("/typeRef/qualifiedName")

    override val args: List<DTOGenericArgument>
        get() = findChildren("/typeRef/genericArgument")

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

package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.DTOGenericArgument
import net.fallingangel.jimmerdto.psi.element.DTOTypeDef
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.util.findChildNullable
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTOGenericArgumentImpl(node: ASTNode) : ANTLRPsiNode(node), DTOGenericArgument {
    override val star: PsiElement?
        get() = findChildNullable("/genericArgument/'*'")

    override val modifier: PsiElement?
        get() = findChildNullable("/genericArgument/Modifier")

    override val type: DTOTypeDef?
        get() = findChildNullable("/genericArgument/typeRef")

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitGenericArgument(this)
        } else {
            super.accept(visitor)
        }
    }
}

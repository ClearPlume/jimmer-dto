package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.*
import net.fallingangel.jimmerdto.psi.mixin.impl.DTONamedElementImpl

class DTOPropNameImpl(node: ASTNode) : DTONamedElementImpl(node), DTOPropName {
    override val value: String
        get() = text

    override fun getNameIdentifier(): PsiElement {
        return this
    }

    override fun newNameNode(name: String): ASTNode {
        return project.createPropName(name).node
    }

    override fun resolve(): PsiElement? {
        return when (val prop = parent) {
            is DTONegativeProp -> prop.property?.source
            is DTOPositiveProp -> prop.property?.source
            else -> null
        }
    }

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitPropName(this)
        } else {
            super.accept(visitor)
        }
    }
}

package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.DTOPositiveProp
import net.fallingangel.jimmerdto.psi.element.DTOPropName
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.psi.element.createPropName
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
        val prop = parent
        if (prop is DTOPositiveProp && prop.arg != null) {
            return null
        }
        return containingLClass?.findProperty(value)?.dependencyItem
    }

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitPropName(this)
        } else {
            super.accept(visitor)
        }
    }
}

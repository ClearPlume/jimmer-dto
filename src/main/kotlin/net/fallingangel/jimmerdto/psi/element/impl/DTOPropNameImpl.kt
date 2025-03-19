package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.*
import net.fallingangel.jimmerdto.psi.mixin.impl.DTONamedElementImpl
import net.fallingangel.jimmerdto.util.file
import net.fallingangel.jimmerdto.util.propPath

class DTOPropNameImpl(node: ASTNode) : DTONamedElementImpl(node), DTOPropName {
    override val value: String
        get() = text

    override fun getNameIdentifier(): PsiElement {
        return this
    }

    override fun getName() = value

    override fun newNameNode(name: String): ASTNode {
        return project.createPropName(name).node
    }

    override fun resolve(): PsiElement? {
        return when (val prop = parent) {
            is DTONegativeProp -> file.clazz.propertyOrNull(prop.propPath())?.source

            is DTOPositiveProp -> if (prop.arg == null) {
                file.clazz.propertyOrNull(prop.propPath())?.source
            } else {
                null
            }

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

package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.*
import net.fallingangel.jimmerdto.psi.mixin.impl.DTONamedElementImpl
import net.fallingangel.jimmerdto.util.file
import net.fallingangel.jimmerdto.util.findChild
import net.fallingangel.jimmerdto.util.findChildNullable
import net.fallingangel.jimmerdto.util.propPath

class DTOPropNameImpl(node: ASTNode) : DTONamedElementImpl(node), DTOPropName {
    override val value: String
        get() {
            val quoted = findChildNullable<PsiElement>("/propName/QuotedSymbol")
            if (quoted != null) {
                val text = quoted.text
                return text.substring(1, text.length - 1)
            }
            return findChild<PsiElement>("/propName/Identifier").text
        }

    override fun getName() = value

    override fun setName(name: String): DTOPropName {
        node.treeParent.replaceChild(node, project.createPropName(name).node)
        return this
    }

    override fun getNameIdentifier(): PsiElement {
        return findChildNullable("/propName/QuotedSymbol") ?: findChild("/propName/Identifier")
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

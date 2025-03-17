package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.DTOAlias
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.psi.element.createAlias
import net.fallingangel.jimmerdto.psi.mixin.impl.DTONamedElementImpl
import net.fallingangel.jimmerdto.refenerce.DTOReference
import net.fallingangel.jimmerdto.util.findChild

class DTOAliasImpl(node: ASTNode) : DTONamedElementImpl(node), DTOAlias {
    override val value: String
        get() = nameIdentifier.text

    override fun getNameIdentifier(): PsiElement {
        return findChild("/alias/Identifier")
    }

    override fun newNameNode(name: String): ASTNode {
        return project.createAlias(name).node
    }

    override fun getReference() = DTOReference(this, textRangeInParent)

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitAlias(this)
        } else {
            super.accept(visitor)
        }
    }
}

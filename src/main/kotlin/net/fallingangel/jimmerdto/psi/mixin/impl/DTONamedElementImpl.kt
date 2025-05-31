package net.fallingangel.jimmerdto.psi.mixin.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.psi.mixin.DTONamedElement
import net.fallingangel.jimmerdto.refenerce.DTOReference
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

abstract class DTONamedElementImpl(node: ASTNode) : ANTLRPsiNode(node), DTONamedElement {
    override fun getName() = nameIdentifier?.text

    override fun setName(newName: String): DTONamedElement {
        newNameNode(newName)?.let { node.treeParent.replaceChild(node, it) }
        return this
    }

    protected open fun newNameNode(name: String): ASTNode? = null

    override fun getReference() = DTOReference(this, firstChild.textRangeInParent)

    override fun resolve(): PsiElement? {
        return null
    }
}
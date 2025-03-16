package net.fallingangel.jimmerdto.psi.mixin.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry
import net.fallingangel.jimmerdto.psi.mixin.DTONamedElement
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

abstract class DTONamedElementImpl(node: ASTNode) : ANTLRPsiNode(node), DTONamedElement {
    override fun getReferences(): Array<PsiReference> {
        return ReferenceProvidersRegistry.getReferencesFromProviders(this)
    }

    override fun getName() = nameIdentifier?.text

    override fun setName(name: String): DTONamedElement {
        newNameNode(name)?.let { node.treeParent.replaceChild(node, it) }
        return this
    }

    protected open fun newNameNode(name: String): ASTNode? = null

    override fun resolve(): PsiElement? {
        return null
    }
}
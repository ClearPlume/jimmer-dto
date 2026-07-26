package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.DTOImported
import net.fallingangel.jimmerdto.psi.element.DTOImportedType
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.psi.element.createImported
import net.fallingangel.jimmerdto.psi.mixin.impl.DTONamedElementImpl
import net.fallingangel.jimmerdto.util.file
import net.fallingangel.jimmerdto.util.findChild
import net.fallingangel.jimmerdto.util.psiClass

class DTOImportedImpl(node: ASTNode) : DTONamedElementImpl(node), DTOImported {
    override val value: String
        get() = nameIdentifier.text

    override fun getNameIdentifier(): PsiElement {
        return findChild("/imported/Identifier")
    }

    override fun newNameNode(name: String): ASTNode {
        return project.createImported(name).node
    }

    override fun resolve(): PsiElement? {
        val parent = parent as DTOImportedType
        val name = parent.alias?.value ?: value
        return file.importIndex[name]?.get(0)?.let { project.psiClass(it) }
    }

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitImported(this)
        } else {
            super.accept(visitor)
        }
    }
}

package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.DTOQualifiedNamePart
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.psi.element.createQualifiedNamePart
import net.fallingangel.jimmerdto.psi.mixin.impl.DTONamedElementImpl
import net.fallingangel.jimmerdto.util.findChildNullable

class DTOQualifiedNamePartImpl(node: ASTNode) : DTONamedElementImpl(node), DTOQualifiedNamePart {
    override val part: String
        get() = nameIdentifier?.text ?: ""

    override fun getNameIdentifier(): PsiElement? {
        return findChildNullable("/qualifiedNamePart/Identifier")
            ?: findChildNullable("/qualifiedNamePart/'like'")
            ?: findChildNullable("/qualifiedNamePart/'null'")
            ?: findChildNullable("/qualifiedNamePart/'desc'")
            ?: findChildNullable("/qualifiedNamePart/'asc'")
    }

    override fun newNameNode(name: String): ASTNode {
        return project.createQualifiedNamePart(name).node
    }

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitQualifiedNamePart(this)
        } else {
            super.accept(visitor)
        }
    }

    override fun resolve(): PsiElement? {
        return target?.source
    }
}

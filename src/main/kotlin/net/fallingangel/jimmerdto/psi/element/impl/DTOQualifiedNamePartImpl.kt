package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.DTOQualifiedNamePart
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.psi.element.createQualifiedNamePart
import net.fallingangel.jimmerdto.psi.mixin.impl.DTONamedElementImpl
import net.fallingangel.jimmerdto.util.findChildNullable
import net.fallingangel.jimmerdto.psi.resolve.Resolution.Target.Property as TargetProperty

class DTOQualifiedNamePartImpl(node: ASTNode) : DTONamedElementImpl(node), DTOQualifiedNamePart {
    override val part: String
        get() = nameIdentifier?.text ?: ""

    override fun getNameIdentifier(): PsiElement? {
        return findChildNullable("/qualifiedNamePart/Identifier")
            ?: findChildNullable("/qualifiedNamePart/Like")
            ?: findChildNullable("/qualifiedNamePart/Null")
            ?: findChildNullable("/qualifiedNamePart/Desc")
            ?: findChildNullable("/qualifiedNamePart/Asc")
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
        val target = target as? TargetProperty ?: return target?.source
        val via = target.via as? TargetProperty.Via.ImplicitId ?: return target.source
        return via.reference.source ?: target.source
    }
}

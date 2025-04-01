package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.*
import net.fallingangel.jimmerdto.psi.mixin.DTONamedElement
import net.fallingangel.jimmerdto.psi.mixin.impl.DTONamedElementImpl
import net.fallingangel.jimmerdto.refenerce.DTOReference
import net.fallingangel.jimmerdto.util.findChild
import net.fallingangel.jimmerdto.util.findChildNullable

class DTOAnnotationParameterImpl(node: ASTNode) : DTONamedElementImpl(node), DTOAnnotationParameter {
    override val name: PsiElement
        get() = findChild("/annotationParameter/Identifier")

    override val eq: PsiElement
        get() = findChild("/annotationParameter/'='")

    override val value: DTOAnnotationValue?
        get() = findChildNullable("/annotationParameter/annotationValue")

    override fun getNameIdentifier() = name

    override fun setName(newName: String): DTONamedElement {
        val newNameNode = project.createAnnotationParameter(newName).name.node
        node.replaceChild(name.node, newNameNode)
        return this
    }

    override fun getReference() = DTOReference(this, name.textRangeInParent)

    override fun resolve(): PsiElement? {
        val anno = parent
        val annoClass = if (anno is DTOAnnotation) {
            anno.qualifiedName.clazz
        } else {
            anno as DTONestAnnotation
            anno.qualifiedName.clazz
        }
        annoClass ?: return null
        return annoClass.methods.find { it.name == name.text }
    }

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitAnnotationParameter(this)
        } else {
            super.accept(visitor)
        }
    }
}

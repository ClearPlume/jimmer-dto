package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.lsi.process
import net.fallingangel.jimmerdto.psi.element.DTOAnnotationParameter
import net.fallingangel.jimmerdto.psi.element.DTOAnnotationValue
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.psi.element.createAnnotationParameter
import net.fallingangel.jimmerdto.psi.mixin.DTOAnnotationElement
import net.fallingangel.jimmerdto.psi.mixin.DTONamedElement
import net.fallingangel.jimmerdto.psi.mixin.impl.DTONamedElementImpl
import net.fallingangel.jimmerdto.psi.resolve.Resolution
import net.fallingangel.jimmerdto.util.findChild
import net.fallingangel.jimmerdto.util.findChildNullable
import net.fallingangel.jimmerdto.util.parent

class DTOAnnotationParameterImpl(node: ASTNode) : DTONamedElementImpl(node), DTOAnnotationParameter {
    override val name: PsiElement
        get() = findChild("/annotationParameter/Identifier")

    override val eq: PsiElement
        get() = findChild("/annotationParameter/Equals")

    override val value: DTOAnnotationValue?
        get() = findChildNullable("/annotationParameter/annotationValue")

    override fun getNameIdentifier() = name

    override fun setName(newName: String): DTONamedElement {
        val newNameNode = project.createAnnotationParameter(newName).name.node
        node.replaceChild(name.node, newNameNode)
        return this
    }

    override fun resolve(): PsiElement? {
        val anno = parent<DTOAnnotationElement>()?.qualifiedName?.target as? Resolution.Target.Type ?: return null
        return process(anno.type) { annotationParam(name.text) }
    }

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitAnnotationParameter(this)
        } else {
            super.accept(visitor)
        }
    }
}

package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.*
import net.fallingangel.jimmerdto.util.findChild
import net.fallingangel.jimmerdto.util.findChildNullable
import net.fallingangel.jimmerdto.util.findChildren
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTOTypeMorphismImpl(node: ASTNode) : ANTLRPsiNode(node), DTOTypeMorphism {
    override val annotations: List<DTOAnnotation>
        get() = findChildren("/typeMorphism/annotation")

    override val targetType: DTOQualifiedName
        get() = findChild("/typeMorphism/qualifiedName")

    override val classDeclaration: DTOClassDeclaration?
        get() = findChildNullable("/typeMorphism/classDeclaration")

    override val implements: DTOImplements?
        get() = findChildNullable("/typeMorphism/implements")

    override val dtoBody: DTODtoBody
        get() = findChild("/typeMorphism/dtoBody")

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitMorphism(this)
        } else {
            super.accept(visitor)
        }
    }
}

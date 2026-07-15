package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.*
import net.fallingangel.jimmerdto.util.findChild
import net.fallingangel.jimmerdto.util.findChildNullable
import net.fallingangel.jimmerdto.util.findChildren
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTOMorphismImpl(node: ASTNode) : ANTLRPsiNode(node), DTOMorphism {
    override val annotations: List<DTOAnnotation>
        get() = findChildren("/morphism/annotation")

    override val modifierElement: PsiElement?
        get() = findChildNullable("/morphism/Modifier")

    override val targetType: DTOQualifiedName?
        get() = findChildNullable("/morphism/qualifiedName")

    override val classDeclaration: DTOClassDeclaration?
        get() = findChildNullable("/morphism/classDeclaration")

    override val implements: DTOImplements?
        get() = findChildNullable("/morphism/implements")

    override val dtoBody: DTODtoBody
        get() = findChild("/morphism/dtoBody")

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitMorphism(this)
        } else {
            super.accept(visitor)
        }
    }
}
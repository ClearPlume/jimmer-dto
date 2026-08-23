package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.*
import net.fallingangel.jimmerdto.util.findChild
import net.fallingangel.jimmerdto.util.findChildNullable
import net.fallingangel.jimmerdto.util.findChildren
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTODefaultMorphismImpl(node: ASTNode) : ANTLRPsiNode(node), DTODefaultMorphism {
    override val annotations: List<DTOAnnotation>
        get() = findChildren("/defaultMorphism/annotation")

    override val default: PsiElement
        get() = findChild("/defaultMorphism/Default")

    override val classDeclaration: DTOClassDeclaration?
        get() = findChildNullable("/defaultMorphism/classDeclaration")

    override val implements: DTOImplements?
        get() = findChildNullable("/defaultMorphism/implements")

    override val dtoBody: DTODtoBody
        get() = findChild("/defaultMorphism/dtoBody")

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitMorphism(this)
        } else {
            super.accept(visitor)
        }
    }
}

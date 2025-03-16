package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.*
import net.fallingangel.jimmerdto.util.findChild
import net.fallingangel.jimmerdto.util.findChildNullable
import net.fallingangel.jimmerdto.util.findChildren
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTODtoImpl(node: ASTNode) : ANTLRPsiNode(node), DTODto {
    override val annotations: List<DTOAnnotation>
        get() = findChildren("/dto/annotation")

    override val modifierElements: List<PsiElement>
        get() = findChildren("/dto/Modifier")

    override val implements: DTOImplements?
        get() = findChildNullable("/dto/implements")

    override val name: DTODtoName
        get() = findChild("/dto/dtoName")

    override val dtoBody: DTODtoBody
        get() = findChild("/dto/dtoBody")

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitDto(this)
        } else {
            super.accept(visitor)
        }
    }
}

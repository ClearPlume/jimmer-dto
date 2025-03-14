package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.*
import net.fallingangel.jimmerdto.util.findChildNullable
import net.fallingangel.jimmerdto.util.findChildren
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTOPropBodyImpl(node: ASTNode) : ANTLRPsiNode(node), DTOPropBody {
    override val annotations: List<DTOAnnotation>
        get() = findChildren("/propBody/annotation")

    override val implements: List<DTOTypeDef>
        get() = findChildren("/propBody/typeRef")

    override val dtoBody: DTODtoBody?
        get() = findChildNullable("/propBody/dtoBody")

    override val enumBody: DTOEnumBody?
        get() = findChildNullable("/propBody/enumBody")

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitPropBody(this)
        } else {
            super.accept(visitor)
        }
    }
}

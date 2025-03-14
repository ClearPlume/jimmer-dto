package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.*
import net.fallingangel.jimmerdto.util.findChildren
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTODtoBodyImpl(node: ASTNode) : ANTLRPsiNode(node), DTODtoBody {
    override val macros: List<DTOMacro>
        get() = findChildren("/dtoBody/macro")

    override val aliasGroups: List<DTOAliasGroup>
        get() = findChildren("/dtoBody/aliasGroup")

    override val positiveProps: List<DTOPositiveProp>
        get() = findChildren("/dtoBody/positiveProp")

    override val negativeProps: List<DTONegativeProp>
        get() = findChildren("/dtoBody/negativeProp")

    override val userProps: List<DTOUserProp>
        get() = findChildren("/dtoBody/userProp")

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitDtoBody(this)
        } else {
            super.accept(visitor)
        }
    }
}

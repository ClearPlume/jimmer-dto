package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.elementType
import net.fallingangel.jimmerdto.DTOLanguage
import net.fallingangel.jimmerdto.psi.DTOParser
import net.fallingangel.jimmerdto.psi.element.DTOAliasGroup
import net.fallingangel.jimmerdto.psi.element.DTOMacro
import net.fallingangel.jimmerdto.psi.element.DTOPositiveProp
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.util.findChild
import net.fallingangel.jimmerdto.util.findChildNullable
import net.fallingangel.jimmerdto.util.findChildren
import net.fallingangel.jimmerdto.util.sibling
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTOAliasGroupImpl(node: ASTNode) : ANTLRPsiNode(node), DTOAliasGroup {
    override val `as`: PsiElement
        get() = findChild("/aliasGroup/'as'")

    override val power: PsiElement?
        get() = findChildNullable("/aliasGroup/'^'")

    override val original: PsiElement?
        get() = arrow.sibling(false) { it.elementType == DTOLanguage.token[DTOParser.Identifier] }

    override val dollar: PsiElement?
        get() = findChildNullable("/aliasGroup/'$'")

    override val arrow: PsiElement
        get() = findChild("/aliasGroup/'->'")

    override val replacement: PsiElement?
        get() = arrow.sibling { it.elementType == DTOLanguage.token[DTOParser.Identifier] }

    override val macros: List<DTOMacro>
        get() = findChildren("/aliasGroup/aliasGroupBody/macro")

    override val positiveProps: List<DTOPositiveProp>
        get() = findChildren("/aliasGroup/aliasGroupBody/positiveProp")

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitAliasGroup(this)
        } else {
            super.accept(visitor)
        }
    }
}

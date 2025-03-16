package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.elementType
import net.fallingangel.jimmerdto.DTOLanguage
import net.fallingangel.jimmerdto.psi.DTOParser
import net.fallingangel.jimmerdto.psi.element.DTOImportedType
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.util.findChild
import net.fallingangel.jimmerdto.util.findChildNullable
import net.fallingangel.jimmerdto.util.sibling
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTOImportedTypeImpl(node: ASTNode) : ANTLRPsiNode(node), DTOImportedType {
    override val type: PsiElement
        get() = findChild("/importedType/Identifier")

    override val alias: PsiElement?
        get() = findChildNullable<PsiElement>("/importedType/'as'")
                ?.sibling { it.elementType == DTOLanguage.token[DTOParser.Identifier] }

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitImportedType(this)
        } else {
            super.accept(visitor)
        }
    }
}

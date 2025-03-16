package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.DTOMacroName
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.psi.mixin.impl.DTONamedElementImpl
import net.fallingangel.jimmerdto.util.findChild

class DTOMacroNameImpl(node: ASTNode) : DTONamedElementImpl(node), DTOMacroName {
    override val value: String
        get() = nameIdentifier.text

    override fun getName() = value

    override fun getNameIdentifier(): PsiElement {
        return findChild("/macroName/Identifier")
    }

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitMacroName(this)
        } else {
            super.accept(visitor)
        }
    }
}

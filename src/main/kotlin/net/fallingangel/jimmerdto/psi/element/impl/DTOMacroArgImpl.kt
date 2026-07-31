package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.DTOMacro
import net.fallingangel.jimmerdto.psi.element.DTOMacroArg
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.psi.element.createMacroArg
import net.fallingangel.jimmerdto.psi.mixin.impl.DTONamedElementImpl
import net.fallingangel.jimmerdto.util.findChildNullable
import net.fallingangel.jimmerdto.util.parent

class DTOMacroArgImpl(node: ASTNode) : DTONamedElementImpl(node), DTOMacroArg {
    override val value: PsiElement?
        get() = nameIdentifier

    override fun getNameIdentifier(): PsiElement? {
        return findChildNullable("/macroArg/Identifier")
    }

    override fun newNameNode(name: String): ASTNode? {
        return project.createMacroArg(name).node
    }

    override fun resolve(): PsiElement? {
        val macro = parent.parent<DTOMacro>()
        val clazz = macro.containingLClass ?: return null
        return if (value?.text == "this" || value?.text == clazz.name) {
            clazz.source
        } else {
            clazz.allParents.find { it.name == value?.text }?.source
        }
    }

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitMacroArg(this)
        } else {
            super.accept(visitor)
        }
    }
}

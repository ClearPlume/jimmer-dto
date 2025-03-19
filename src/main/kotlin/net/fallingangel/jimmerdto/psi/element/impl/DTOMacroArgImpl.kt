package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.Computable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import net.fallingangel.jimmerdto.lsi.LClass
import net.fallingangel.jimmerdto.psi.element.DTOMacro
import net.fallingangel.jimmerdto.psi.element.DTOMacroArg
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.psi.element.createMacroArg
import net.fallingangel.jimmerdto.psi.mixin.impl.DTONamedElementImpl
import net.fallingangel.jimmerdto.util.*

class DTOMacroArgImpl(node: ASTNode) : DTONamedElementImpl(node), DTOMacroArg {
    private val clazz: LClass<*>
        get() = CachedValuesManager.getCachedValue(this) {
            val macro = parent.parent<DTOMacro>()
            val clazz = file.clazz.walk(macro.propPath())
            CachedValueProvider.Result.create(clazz, virtualFile, DumbService.getInstance(project).modificationTracker)
        }

    override val value: PsiElement
        get() = nameIdentifier

    override fun getNameIdentifier(): PsiElement {
        return findChild("/macroArg/Identifier")
    }

    override fun newNameNode(name: String): ASTNode? {
        if ((value.text == "this" || value.text == clazz.name) && !file.hasExport) {
            return WriteCommandAction.runWriteCommandAction(
                project,
                Computable {
                    virtualFile.rename(this, "$name.dto")
                    project.createMacroArg(name).node
                },
            )
        }
        return project.createMacroArg(name).node
    }

    override fun resolve(): PsiElement? {
        return if (value.text == "this" || value.text == clazz.name) {
            clazz.source
        } else {
            clazz.allParents.find { it.name == value.text }?.source
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

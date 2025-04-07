package net.fallingangel.jimmerdto.psi.fix

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import net.fallingangel.jimmerdto.psi.element.DTOQualifiedName
import net.fallingangel.jimmerdto.psi.element.createQualifiedNamePart

class ReplaceIdAccessorToView(private val element: DTOQualifiedName, private val old: String, private val new: String) : BaseFix() {
    override fun getText() = "Replace `$old` to `$new`"

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        val newPart = project.createQualifiedNamePart(new)

        WriteCommandAction.runWriteCommandAction(project) {
            element.parts.last().prevSibling.delete()
            element.parts.last().delete()
            element.parts.last().replace(newPart)
        }
    }
}
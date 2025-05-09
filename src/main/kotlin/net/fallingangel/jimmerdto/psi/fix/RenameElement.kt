package net.fallingangel.jimmerdto.psi.fix

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.ui.components.JBTextField
import net.fallingangel.jimmerdto.psi.mixin.DTOElement

class RenameElement(private val element: PsiElement, private val newElement: Project.(String) -> DTOElement) : BaseFix() {
    override fun getText() = "Rename element"

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        val input = JBTextField(element.text)
        // 是否确认输入信息
        val conformedInput = DialogBuilder(project).apply {
            setTitle("Type New Name")
            centerPanel(input)
        }.showAndGet()
        if (!conformedInput) {
            return
        }
        WriteCommandAction.runWriteCommandAction(project) {
            element.replace(project.newElement(input.text))
        }
    }
}
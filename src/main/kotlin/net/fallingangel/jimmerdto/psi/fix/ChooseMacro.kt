package net.fallingangel.jimmerdto.psi.fix

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import net.fallingangel.jimmerdto.psi.element.createMacroName
import net.fallingangel.jimmerdto.util.popupChooser

class ChooseMacro(private val element: PsiElement) : BaseFix() {
    override fun getText() = "Choose new macro name"

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        editor.popupChooser("Macros", listOf("allScalars", "allReferences")) {
            WriteCommandAction.runWriteCommandAction(project) {
                element.replace(project.createMacroName(it))
            }
        }
    }
}
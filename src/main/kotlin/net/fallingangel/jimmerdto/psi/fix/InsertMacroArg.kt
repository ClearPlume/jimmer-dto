package net.fallingangel.jimmerdto.psi.fix

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import net.fallingangel.jimmerdto.psi.element.DTOMacro
import net.fallingangel.jimmerdto.psi.element.createMacroArg
import net.fallingangel.jimmerdto.util.popupChooser

class InsertMacroArg(private val element: DTOMacro) : BaseFix() {
    override fun getText() = "Insert macro arg"

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        val args = element.args ?: return

        editor.popupChooser("Macro Arg to Insert", element.types) {
            WriteCommandAction.runWriteCommandAction(project) {
                args.addBefore(project.createMacroArg(it), args.r)
            }
        }
    }
}
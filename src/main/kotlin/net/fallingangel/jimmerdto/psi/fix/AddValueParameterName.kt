package net.fallingangel.jimmerdto.psi.fix

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import net.fallingangel.jimmerdto.psi.element.DTOAnnotationValue
import net.fallingangel.jimmerdto.psi.element.createAnnotationParameter

class AddValueParameterName(private val element: DTOAnnotationValue) : BaseFix() {
    override fun getText() = "Add `value = ` to param"

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        val param = project.createAnnotationParameter("value", element.text)
        WriteCommandAction.runWriteCommandAction(project) {
            element.replace(param)
        }
    }
}
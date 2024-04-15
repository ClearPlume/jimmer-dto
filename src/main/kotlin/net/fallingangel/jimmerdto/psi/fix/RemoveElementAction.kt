package net.fallingangel.jimmerdto.psi.fix

import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import net.fallingangel.jimmerdto.psi.DTOFile

class RemoveElementAction(private val name: String, private val element: PsiElement) : BaseIntentionAction() {
    override fun getFamilyName() = "Remove $name"

    override fun getText() = "Remove $name"

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        if (editor == null || file == null) {
            return false
        }
        return file is DTOFile
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null || file == null) {
            return
        }
        element.parent.node.removeChild(element.node)
    }
}
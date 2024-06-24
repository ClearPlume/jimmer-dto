package net.fallingangel.jimmerdto.psi.fix

import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import net.fallingangel.jimmerdto.psi.DTOFile

abstract class BaseFix : BaseIntentionAction() {
    override fun startInWriteAction() = false

    override fun getFamilyName() = "JimmerDTO fix action"

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        if (editor == null || file == null) {
            return false
        }
        return file is DTOFile && isAvailable()
    }

    open fun isAvailable() = true
}
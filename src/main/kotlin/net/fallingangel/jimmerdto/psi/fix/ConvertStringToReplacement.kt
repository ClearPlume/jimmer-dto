package net.fallingangel.jimmerdto.psi.fix

import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import net.fallingangel.jimmerdto.psi.DTOFile
import net.fallingangel.jimmerdto.psi.createAliasGroupReplacement

class ConvertStringToReplacement(private val stringConstant: PsiElement) : BaseIntentionAction() {
    override fun getFamilyName() = "Convert string to replacement"

    override fun getText() = "Convert string to replacement"

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

        if (stringConstant.text == "\"\"") {
            stringConstant.parent.node.removeChild(stringConstant.node)
        } else {
            stringConstant.parent.node.replaceChild(
                stringConstant.node,
                project.createAliasGroupReplacement(stringConstant.text.substring(1, stringConstant.text.lastIndex))!!.node
            )
        }
    }
}
package net.fallingangel.jimmerdto.psi.fix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import net.fallingangel.jimmerdto.psi.createAliasGroupReplacement

class ConvertStringToReplacement(private val stringConstant: PsiElement) : BaseFix() {
    override fun getText() = "Convert string to replacement"

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
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
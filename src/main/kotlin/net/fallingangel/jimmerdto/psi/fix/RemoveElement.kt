package net.fallingangel.jimmerdto.psi.fix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class RemoveElement(private val name: String, private val element: PsiElement) : BaseFix() {
    override fun getText() = "Remove `$name`"

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        element.parent.node.removeChild(element.node)
    }
}
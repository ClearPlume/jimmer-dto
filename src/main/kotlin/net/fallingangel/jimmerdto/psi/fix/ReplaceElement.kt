package net.fallingangel.jimmerdto.psi.fix

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class ReplaceElement(private val element: PsiElement, private val newElement: PsiElement) : BaseFix() {
    override fun getText() = "Replace `${element.text}` to `${newElement.text}`"

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        WriteCommandAction.runWriteCommandAction(project) {
            element.node.treeParent.replaceChild(element.node, newElement.node)
        }
    }
}
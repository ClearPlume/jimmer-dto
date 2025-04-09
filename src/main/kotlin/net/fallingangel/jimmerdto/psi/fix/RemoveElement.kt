package net.fallingangel.jimmerdto.psi.fix

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

/**
 * @param targetSelector 纯函数
 * @param relatedElementsFinder 纯函数
 */
class RemoveElement(
    private val displayName: String,
    private val anchor: PsiElement,
    private val targetSelector: (PsiElement) -> PsiElement = { it },
    private val relatedElementsFinder: (PsiElement) -> List<PsiElement> = { emptyList() },
) : BaseFix() {
    override fun getText() = "Remove `$displayName`"

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        WriteCommandAction.runWriteCommandAction(project) {
            val relatedElements = relatedElementsFinder(anchor)
            targetSelector(anchor).delete()
            relatedElements.forEach(PsiElement::delete)
        }
    }
}
package net.fallingangel.jimmerdto.psi.fix

import com.intellij.modcommand.ActionContext
import com.intellij.modcommand.ModPsiUpdater
import com.intellij.modcommand.PsiUpdateModCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

@Suppress("UnstableApiUsage")
class InsertAfter(
    anchor: PsiElement,
    private val displayName: String,
    private val newElement: Project.(String) -> PsiElement,
) : PsiUpdateModCommandAction<PsiElement>(anchor) {
    override fun getFamilyName() = "Insert '$displayName'"

    override fun invoke(context: ActionContext, anchor: PsiElement, updater: ModPsiUpdater) {
        anchor.parent.addAfter(anchor.project.newElement(displayName), anchor)
    }
}

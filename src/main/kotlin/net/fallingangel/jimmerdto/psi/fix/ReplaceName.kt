package net.fallingangel.jimmerdto.psi.fix

import com.intellij.modcommand.ActionContext
import com.intellij.modcommand.ModPsiUpdater
import com.intellij.modcommand.PsiUpdateModCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

@Suppress("UnstableApiUsage")
class ReplaceName(
    element: PsiElement, private val newName: String, private val newElement: Project.(String) -> PsiElement
) : PsiUpdateModCommandAction<PsiElement>(element) {
    override fun getFamilyName() = "Replace to `$newName`"

    override fun invoke(context: ActionContext, element: PsiElement, updater: ModPsiUpdater) {
        element.replace(context.project.newElement(newName))
    }
}
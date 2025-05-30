package net.fallingangel.jimmerdto.psi.fix

import com.intellij.modcommand.ActionContext
import com.intellij.modcommand.ModPsiUpdater
import com.intellij.modcommand.PsiUpdateModCommandAction
import com.intellij.psi.PsiElement

/**
 * @param targetSelector 纯函数
 * @param relatedElementsFinder 纯函数
 */
@Suppress("UnstableApiUsage")
class RemoveElement(
    private val displayName: String,
    anchor: PsiElement,
    private val targetSelector: (PsiElement) -> PsiElement = { it },
    private val relatedElementsFinder: (PsiElement) -> List<PsiElement> = { emptyList() },
) : PsiUpdateModCommandAction<PsiElement>(anchor) {
    override fun getFamilyName() = "Remove `$displayName`"

    override fun invoke(context: ActionContext, element: PsiElement, updater: ModPsiUpdater) {
        val target = targetSelector(element)
        val relatedElements = relatedElementsFinder(target)
        target.delete()
        relatedElements.forEach(PsiElement::delete)
    }
}

package net.fallingangel.jimmerdto.psi.fix

import com.intellij.modcommand.ActionContext
import com.intellij.modcommand.ModPsiUpdater
import com.intellij.modcommand.PsiUpdateModCommandAction
import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.psi.DTOLexer

@Suppress("UnstableApiUsage")
class RemoveElement(
    private val displayName: String,
    element: PsiElement,
    private val relatedTokenType: Int = DTOLexer.Comma,
) : PsiUpdateModCommandAction<PsiElement>(element) {
    override fun getFamilyName() = "Remove '$displayName'"

    override fun invoke(context: ActionContext, element: PsiElement, updater: ModPsiUpdater) {
        element.deleteWithAdjacentToken(relatedTokenType)
    }
}

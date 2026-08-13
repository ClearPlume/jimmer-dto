package net.fallingangel.jimmerdto.psi.fix

import com.intellij.modcommand.ActionContext
import com.intellij.modcommand.ModPsiUpdater
import com.intellij.modcommand.PsiUpdateModCommandAction
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import net.fallingangel.jimmerdto.core.DTOLanguage
import net.fallingangel.jimmerdto.psi.DTOLexer
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespace
import org.jetbrains.kotlin.psi.psiUtil.getPrevSiblingIgnoringWhitespace

@Suppress("UnstableApiUsage")
class RemoveElement(
    private val displayName: String,
    element: PsiElement,
    private val relatedTokenType: Int = DTOLexer.Comma,
) : PsiUpdateModCommandAction<PsiElement>(element) {
    override fun getFamilyName() = "Remove '$displayName'"

    override fun invoke(context: ActionContext, element: PsiElement, updater: ModPsiUpdater) {
        val relatedElementType = DTOLanguage.token[relatedTokenType]
        val related = element.getNextSiblingIgnoringWhitespace(false)?.takeIf { it.elementType == relatedElementType }
            ?: element.getPrevSiblingIgnoringWhitespace(false)?.takeIf { it.elementType == relatedElementType }
        related?.delete()

        element.delete()
    }
}

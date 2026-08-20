package net.fallingangel.jimmerdto.psi.fix

import com.intellij.modcommand.ActionContext
import com.intellij.modcommand.ModPsiUpdater
import com.intellij.modcommand.PsiUpdateModCommandAction
import net.fallingangel.jimmerdto.psi.element.DTOQualifiedName

@Suppress("UnstableApiUsage")
class RemoveRedundantQualifierNameFix(
    qualifiedName: DTOQualifiedName,
    private val retainedIndex: Int,
) : PsiUpdateModCommandAction<DTOQualifiedName>(qualifiedName) {
    override fun getFamilyName() = "Remove redundant qualifier name"

    override fun invoke(context: ActionContext, element: DTOQualifiedName, updater: ModPsiUpdater) {
        val parts = element.parts
        element.deleteChildRange(parts.first(), parts[retainedIndex - 1].nextSibling)
    }
}

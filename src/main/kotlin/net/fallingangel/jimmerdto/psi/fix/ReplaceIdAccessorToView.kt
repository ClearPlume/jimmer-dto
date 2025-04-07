package net.fallingangel.jimmerdto.psi.fix

import com.intellij.modcommand.ActionContext
import com.intellij.modcommand.ModPsiUpdater
import com.intellij.modcommand.PsiUpdateModCommandAction
import net.fallingangel.jimmerdto.psi.element.DTOQualifiedName
import net.fallingangel.jimmerdto.psi.element.createQualifiedNamePart

@Suppress("UnstableApiUsage")
class ReplaceIdAccessorToView(
    element: DTOQualifiedName,
    private val old: String,
    private val new: String,
) : PsiUpdateModCommandAction<DTOQualifiedName>(element) {
    override fun getFamilyName() = "Replace `$old` to `$new`"

    override fun invoke(context: ActionContext, element: DTOQualifiedName, updater: ModPsiUpdater) {
        val project = context.project
        val newPart = project.createQualifiedNamePart(new)

        element.parts.last().prevSibling.delete()
        element.parts.last().delete()
        element.parts.last().replace(newPart)
    }
}
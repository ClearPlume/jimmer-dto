package net.fallingangel.jimmerdto.psi.fix

import com.intellij.modcommand.ActionContext
import com.intellij.modcommand.ModPsiUpdater
import com.intellij.modcommand.PsiUpdateModCommandAction
import com.intellij.psi.util.startOffset
import net.fallingangel.jimmerdto.psi.element.DTOPositiveProp
import net.fallingangel.jimmerdto.psi.element.createDtoBody

@Suppress("UnstableApiUsage")
class AddDtoBody(element: DTOPositiveProp, val functionName: String) : PsiUpdateModCommandAction<DTOPositiveProp>(element) {
    override fun getFamilyName(): String {
        return "Add '$functionName' body"
    }

    override fun invoke(context: ActionContext, element: DTOPositiveProp, updater: ModPsiUpdater) {
        val project = context.project
        val functionBody = project.createDtoBody()
        val addedBody = element.add(functionBody)
        updater.moveCaretTo(addedBody.startOffset + 1)
    }
}
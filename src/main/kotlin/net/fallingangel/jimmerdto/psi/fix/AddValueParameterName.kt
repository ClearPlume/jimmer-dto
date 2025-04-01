package net.fallingangel.jimmerdto.psi.fix

import com.intellij.modcommand.ActionContext
import com.intellij.modcommand.ModPsiUpdater
import com.intellij.modcommand.PsiUpdateModCommandAction
import net.fallingangel.jimmerdto.psi.element.DTOAnnotationValue
import net.fallingangel.jimmerdto.psi.element.createAnnotationParameter

@Suppress("UnstableApiUsage")
class AddValueParameterName(element: DTOAnnotationValue) : PsiUpdateModCommandAction<DTOAnnotationValue>(element) {
    override fun getFamilyName() = "Add `value = ` to param"

    override fun invoke(context: ActionContext, element: DTOAnnotationValue, updater: ModPsiUpdater) {
        val project = context.project
        val param = project.createAnnotationParameter("value", element.text)

        element.replace(param)
    }
}
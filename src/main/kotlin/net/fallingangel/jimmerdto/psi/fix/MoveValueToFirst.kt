package net.fallingangel.jimmerdto.psi.fix

import com.intellij.modcommand.ActionContext
import com.intellij.modcommand.ModPsiUpdater
import com.intellij.modcommand.PsiUpdateModCommandAction
import net.fallingangel.jimmerdto.psi.element.createAnnotationParameter
import net.fallingangel.jimmerdto.psi.element.createAnnotationValue
import net.fallingangel.jimmerdto.psi.mixin.DTOAnnotationElement

@Suppress("UnstableApiUsage")
class MoveValueToFirst(element: DTOAnnotationElement) : PsiUpdateModCommandAction<DTOAnnotationElement>(element) {
    override fun getFamilyName(): String {
        return "Move to first position"
    }

    override fun invoke(context: ActionContext, element: DTOAnnotationElement, updater: ModPsiUpdater) {
        val project = context.project
        val params = element.params

        val firstParam = params.first()
        val firstNext = firstParam.nextSibling
        val value = element.value!!
        val valueNext = value.nextSibling

        val newValue = project.createAnnotationValue(value.text)
        val newFirstParam = project.createAnnotationParameter(firstParam.name.text, firstParam.value!!.text)

        value.delete()
        firstParam.delete()

        element.addBefore(newValue, firstNext)
        element.addBefore(newFirstParam, valueNext)
    }
}
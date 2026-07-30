package net.fallingangel.jimmerdto.psi.fix

import com.intellij.modcommand.ActionContext
import com.intellij.modcommand.ModPsiUpdater
import com.intellij.modcommand.PsiUpdateModCommandAction
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotation
import net.fallingangel.jimmerdto.psi.element.createAnnotation
import net.fallingangel.jimmerdto.psi.mixin.DTOAnnotationElement

@Suppress("UnstableApiUsage")
class AddAllAnnotationParams(
    element: DTOAnnotationElement,
    private val missedParams: List<LAnnotation.Param>,
) : PsiUpdateModCommandAction<DTOAnnotationElement>(element) {
    override fun getFamilyName(): String {
        return "Add all missing parameters"
    }

    override fun invoke(context: ActionContext, element: DTOAnnotationElement, updater: ModPsiUpdater) {
        val existing = listOfNotNull(element.value?.text) + element.params.map { it.text }
        val added = missedParams.map { "${it.name} = ${it.type.placeholder}" }
        val annotation = context.project.createAnnotation(element.qualifiedName.value, existing + added)
        val inserted = element.replace(annotation) as DTOAnnotationElement

        val builder = updater.templateBuilder()
        inserted.params
            .takeLast(added.size)
            .zip(missedParams)
            .forEach { (parameter, param) ->
                builder.field(parameter.value!!, param.type.templateExpression)
            }
    }
}
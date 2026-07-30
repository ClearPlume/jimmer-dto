package net.fallingangel.jimmerdto.psi.fix

import com.intellij.modcommand.ActionContext
import com.intellij.modcommand.ModPsiUpdater
import com.intellij.modcommand.PsiUpdateModCommandAction
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotation
import net.fallingangel.jimmerdto.psi.element.DTOAnnotationParameter
import net.fallingangel.jimmerdto.psi.element.createAnnotation
import net.fallingangel.jimmerdto.psi.element.createAnnotationParameter
import net.fallingangel.jimmerdto.psi.element.createComma
import net.fallingangel.jimmerdto.psi.mixin.DTOAnnotationElement

@Suppress("UnstableApiUsage")
class AddAnnotationParam(
    element: DTOAnnotationElement,
    private val param: LAnnotation.Param,
) : PsiUpdateModCommandAction<DTOAnnotationElement>(element) {
    override fun getFamilyName(): String {
        return "Add parameter '${param.name}'"
    }

    override fun invoke(context: ActionContext, element: DTOAnnotationElement, updater: ModPsiUpdater) {
        val project = context.project
        val leftBrace = element.qualifiedName.nextSibling
        val rightBrace = leftBrace?.let { element.lastChild }
        val builder = updater.templateBuilder()

        // 无参数时
        if (rightBrace == null) {
            val annotation = project.createAnnotation(element.qualifiedName.value, listOf("${param.name} = ${param.type.placeholder}"))
            val insertedAnnotation = element.replace(annotation) as DTOAnnotationElement
            builder.field(insertedAnnotation.params[0].value!!, param.type.templateExpression)
            return
        }

        // 补充参数
        val parameter = project.createAnnotationParameter(param.name, param.type.placeholder)

        // @Foo("x") / @Foo(value = "x")
        if (element.params.isNotEmpty() || element.value != null) {
            element.addBefore(project.createComma(), rightBrace)
        }

        val insertedParameter = element.addBefore(parameter, rightBrace) as DTOAnnotationParameter
        builder.field(insertedParameter.value!!, param.type.templateExpression)
    }
}
package net.fallingangel.jimmerdto.psi.fix

import com.intellij.codeInsight.template.impl.EmptyNode
import com.intellij.modcommand.ActionContext
import com.intellij.modcommand.ModPsiUpdater
import com.intellij.modcommand.PsiUpdateModCommandAction
import com.intellij.psi.PsiAnnotationMethod
import net.fallingangel.jimmerdto.psi.element.DTOAnnotationParameter
import net.fallingangel.jimmerdto.psi.element.createAnnotationParameter
import net.fallingangel.jimmerdto.psi.element.createComma
import net.fallingangel.jimmerdto.psi.mixin.DTOElement
import net.fallingangel.jimmerdto.util.defaultValue

/**
 * 注解元素可能是DTOAnnotation，也可能是DTONestAnnotation
 */
@Suppress("UnstableApiUsage")
class GenerateMissingAnnotationParam(
    element: DTOElement,
    private val params: Collection<PsiAnnotationMethod>,
) : PsiUpdateModCommandAction<DTOElement>(element) {
    override fun getFamilyName() = if (params.size == 1) {
        val name = params.first().name
        "Generate missing param: `$name`"
    } else {
        val params = params.joinToString(transform = PsiAnnotationMethod::getName)
        "Generate missing params: $params"
    }

    override fun invoke(context: ActionContext, element: DTOElement, updater: ModPsiUpdater) {
        val paren = element.lastChild
        val project = context.project
        val builder = updater.templateBuilder()

        val insertedParams = params.map { param ->
            val parameter = project.createAnnotationParameter(param.name, param.returnType.defaultValue)
            val comma = project.createComma()

            element.addBefore(comma, paren)
            element.addBefore(parameter, paren)
        }
        insertedParams
                .filterIsInstance<DTOAnnotationParameter>()
                // 此处param为手动生成，不存在空
                .forEach { builder.field(it.value!!, EmptyNode()) }
    }
}
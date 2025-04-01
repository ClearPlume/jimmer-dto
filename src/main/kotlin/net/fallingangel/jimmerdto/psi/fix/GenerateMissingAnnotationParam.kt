package net.fallingangel.jimmerdto.psi.fix

import com.intellij.codeInsight.template.TemplateBuilderFactory
import com.intellij.codeInsight.template.impl.EmptyNode
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.psi.PsiAnnotationMethod
import com.intellij.psi.PsiFile
import net.fallingangel.jimmerdto.psi.element.DTOAnnotationParameter
import net.fallingangel.jimmerdto.psi.element.createAnnotationParameter
import net.fallingangel.jimmerdto.psi.element.createComma
import net.fallingangel.jimmerdto.psi.mixin.DTOElement
import net.fallingangel.jimmerdto.util.defaultValue

/**
 * 注解元素可能是DTOAnnotation，也可能是DTONestAnnotation
 */
class GenerateMissingAnnotationParam(private val element: DTOElement, private val params: Collection<PsiAnnotationMethod>) : BaseFix() {
    override fun getText() = if (params.size == 1) {
        val name = params.first().name
        "Generate missing param: `$name`"
    } else {
        val params = params.joinToString(transform = PsiAnnotationMethod::getName)
        "Generate missing params: $params"
    }

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        val paren = element.lastChild
        val builder = TemplateBuilderFactory.getInstance().createTemplateBuilder(element)

        val insertedParams = WriteCommandAction.runWriteCommandAction(
            project,
            Computable {
                params.map { param ->
                    val parameter = project.createAnnotationParameter(param.name, param.returnType.defaultValue)
                    val comma = project.createComma()

                    element.addBefore(comma, paren)
                    element.addBefore(parameter, paren)
                }
            },
        )
        insertedParams
                .filterIsInstance<DTOAnnotationParameter>()
                // 此处param为手动生成，不存在空
                .forEach { builder.replaceElement(it.value!!, EmptyNode()) }
    }
}
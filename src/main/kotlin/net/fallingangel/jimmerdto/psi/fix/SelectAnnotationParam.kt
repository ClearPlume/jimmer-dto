package net.fallingangel.jimmerdto.psi.fix

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import net.fallingangel.jimmerdto.psi.element.DTOAnnotation
import net.fallingangel.jimmerdto.psi.element.DTOAnnotationParameter
import net.fallingangel.jimmerdto.psi.element.DTONestAnnotation
import net.fallingangel.jimmerdto.psi.element.createAnnotationParameter
import net.fallingangel.jimmerdto.util.popupChooser

class SelectAnnotationParam(private val element: PsiElement) : BaseFix() {
    override fun getText() = "Select param"

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        val anno = element.parent
        val params = if (anno is DTOAnnotation) {
            anno.qualifiedName.clazz?.methods?.map { it.name } ?: emptyList()
        } else {
            anno as DTONestAnnotation
            anno.qualifiedName.clazz?.methods?.map { it.name } ?: emptyList()
        }

        editor.popupChooser("Select a param", params) {
            val paramNode = if (element is DTOAnnotationParameter) {
                // 在调用之前，已经确认此处非空
                project.createAnnotationParameter(it, element.value!!.text).node
            } else {
                project.createAnnotationParameter(it, element.text).node
            }
            WriteCommandAction.runWriteCommandAction(project) {
                anno.node.replaceChild(element.node, paramNode)
            }
        }
    }
}

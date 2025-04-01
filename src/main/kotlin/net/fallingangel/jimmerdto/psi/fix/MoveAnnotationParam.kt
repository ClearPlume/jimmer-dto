package net.fallingangel.jimmerdto.psi.fix

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import net.fallingangel.jimmerdto.psi.element.*

class MoveAnnotationParam(private val element: DTOAnnotationValue) : BaseFix() {
    override fun getText() = "Move to first position"

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        val anno = element.parent
        val params = if (anno is DTOAnnotation) {
            anno.params
        } else {
            anno as DTONestAnnotation
            anno.params
        }

        val first = params.first()
        val firstNext = first.nextSibling
        val elemNext = element.nextSibling

        val newElement = project.createAnnotationValue(element.text)
        // 调用前已经判空保证参数有值
        val newFirst = project.createAnnotationParameter(first.name.text, first.value!!.text)

        WriteCommandAction.runWriteCommandAction(project) {
            element.delete()
            first.delete()

            anno.addBefore(newElement, firstNext)
            anno.addBefore(newFirst, elemNext)
        }
    }
}
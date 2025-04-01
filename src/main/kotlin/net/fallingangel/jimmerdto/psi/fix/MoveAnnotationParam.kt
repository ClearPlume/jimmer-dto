package net.fallingangel.jimmerdto.psi.fix

import com.intellij.modcommand.ActionContext
import com.intellij.modcommand.ModPsiUpdater
import com.intellij.modcommand.PsiUpdateModCommandAction
import net.fallingangel.jimmerdto.psi.element.*

@Suppress("UnstableApiUsage")
class MoveAnnotationParam(element: DTOAnnotationValue) : PsiUpdateModCommandAction<DTOAnnotationValue>(element) {
    override fun getFamilyName() = "Move to first position"

    override fun invoke(context: ActionContext, element: DTOAnnotationValue, updater: ModPsiUpdater) {
        val anno = element.parent
        val params = if (anno is DTOAnnotation) {
            anno.params
        } else {
            anno as DTONestAnnotation
            anno.params
        }

        val project = context.project
        val first = params.first()
        val firstNext = first.nextSibling
        val elemNext = element.nextSibling

        val newElement = project.createAnnotationValue(element.text)
        // 调用前已经判空保证参数有值
        val newFirst = project.createAnnotationParameter(first.name.text, first.value!!.text)

        element.delete()
        first.delete()

        anno.addBefore(newElement, firstNext)
        anno.addBefore(newFirst, elemNext)
    }
}
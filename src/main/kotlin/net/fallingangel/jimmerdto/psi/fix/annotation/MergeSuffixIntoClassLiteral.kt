package net.fallingangel.jimmerdto.psi.fix.annotation

import com.intellij.modcommand.ActionContext
import com.intellij.modcommand.ModPsiUpdater
import com.intellij.modcommand.PsiUpdateModCommandAction
import net.fallingangel.jimmerdto.psi.element.DTOAnnotationSingleValue
import net.fallingangel.jimmerdto.psi.element.createToken

@Suppress("UnstableApiUsage")
class MergeSuffixIntoClassLiteral(
    annotationValue: DTOAnnotationSingleValue,
) : PsiUpdateModCommandAction<DTOAnnotationSingleValue>(annotationValue) {
    override fun getFamilyName() = "Merge suffix into class literal"

    override fun invoke(context: ActionContext, element: DTOAnnotationSingleValue, updater: ModPsiUpdater) {
        val classSuffix = element.classSuffix ?: return
        val project = context.project
        classSuffix.classOperator.replace(project.createToken("."))
        classSuffix.add(project.createToken("::"))
        classSuffix.add(project.createToken("class"))
    }
}

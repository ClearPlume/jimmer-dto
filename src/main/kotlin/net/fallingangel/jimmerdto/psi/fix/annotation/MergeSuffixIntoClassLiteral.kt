package net.fallingangel.jimmerdto.psi.fix.annotation

import com.intellij.modcommand.ActionContext
import com.intellij.modcommand.ModPsiUpdater
import com.intellij.modcommand.PsiUpdateModCommandAction
import net.fallingangel.jimmerdto.psi.element.DTOAnnotationSingleValue
import net.fallingangel.jimmerdto.psi.element.createClassOperator
import net.fallingangel.jimmerdto.psi.element.createClassKeyword
import net.fallingangel.jimmerdto.psi.element.createDot

@Suppress("UnstableApiUsage")
class MergeSuffixIntoClassLiteral(
    annotationValue: DTOAnnotationSingleValue,
) : PsiUpdateModCommandAction<DTOAnnotationSingleValue>(annotationValue) {
    override fun getFamilyName() = "Merge suffix into class literal"

    override fun invoke(context: ActionContext, element: DTOAnnotationSingleValue, updater: ModPsiUpdater) {
        val classSuffix = element.classSuffix ?: return
        val project = context.project
        classSuffix.classOperator.replace(project.createDot())
        classSuffix.add(project.createClassOperator())
        classSuffix.add(project.createClassKeyword())
    }
}

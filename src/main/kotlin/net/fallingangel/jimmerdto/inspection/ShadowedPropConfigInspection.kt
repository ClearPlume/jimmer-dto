package net.fallingangel.jimmerdto.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.DTOPositiveProp
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.psi.fix.RemoveElement
import net.fallingangel.jimmerdto.psi.fix.asQuickFix

class ShadowedPropConfigInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : DTOVisitor() {
            override fun visitPositiveProp(o: DTOPositiveProp) {
                o.configs
                    .groupBy { it.name.text }
                    .forEach { (_, configs) ->
                        configs.dropLast(1).forEach {
                            val displayName = it.name.text
                            holder.registerProblem(
                                it,
                                "Duplicated prop config '$displayName', only the last one takes effect",
                                ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                                RemoveElement(displayName, it).asQuickFix(),
                            )
                        }
                    }
            }
        }
    }
}
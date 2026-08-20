package net.fallingangel.jimmerdto.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.DTOQualifiedName
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.psi.fix.RemoveRedundantQualifierNameFix
import net.fallingangel.jimmerdto.psi.fix.asQuickFix
import net.fallingangel.jimmerdto.psi.resolve.Resolution
import net.fallingangel.jimmerdto.util.file

class RedundantQualifierNameInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : DTOVisitor() {
            override fun visitQualifiedName(o: DTOQualifiedName) {
                if (o.initialSpace is Resolution.Space.GlobalRaw) return

                val parts = o.parts

                val retainedIndex = parts.indices.reversed().firstOrNull { parts[it].part in o.file.importIndex } ?: return
                if (retainedIndex == 0) return

                holder.registerProblem(
                    o,
                    "Redundant qualifier name",
                    ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                    TextRange.create(0, parts[retainedIndex].textRangeInParent.startOffset),
                    RemoveRedundantQualifierNameFix(o, retainedIndex).asQuickFix(),
                )
            }
        }
    }
}

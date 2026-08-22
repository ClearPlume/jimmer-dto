package net.fallingangel.jimmerdto.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.lsi.Precompiler
import net.fallingangel.jimmerdto.lsi.jimmer.JimmerAnnotations
import net.fallingangel.jimmerdto.lsi.process
import net.fallingangel.jimmerdto.psi.element.DTOAnnotation
import net.fallingangel.jimmerdto.psi.element.DTODto
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.psi.fix.RemoveElement
import net.fallingangel.jimmerdto.psi.fix.asQuickFix
import net.fallingangel.jimmerdto.util.file

class IgnoredKotlinDtoInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : DTOVisitor() {
            override fun visitAnnotation(o: DTOAnnotation) {
                val target = o.qualifiedName.target ?: return
                if (process(target.source) { className() } != JimmerAnnotations.KotlinDto) return

                if (o.file.precompiler != Precompiler.Ksp) {
                    holder.registerProblem(
                        o,
                        "@KotlinDto is ignored",
                        ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                        RemoveElement(o.qualifiedName.value, o).asQuickFix(),
                    )
                    return
                }

                if (o.host !is DTODto) {
                    holder.registerProblem(
                        o,
                        "@KotlinDto only takes effect on DTO type declarations",
                        ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                        RemoveElement(o.qualifiedName.value, o).asQuickFix(),
                    )
                    return
                }
            }
        }
    }
}

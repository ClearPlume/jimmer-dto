package net.fallingangel.jimmerdto.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotationSite
import net.fallingangel.jimmerdto.lsi.jimmer.JimmerAnnotations
import net.fallingangel.jimmerdto.lsi.process
import net.fallingangel.jimmerdto.psi.element.DTOAnnotation
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.psi.fix.RemoveElement
import net.fallingangel.jimmerdto.psi.fix.asQuickFix
import net.fallingangel.jimmerdto.util.file

class DiscardedAnnotationInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : DTOVisitor() {
            override fun visitAnnotation(o: DTOAnnotation) {
                val target = o.qualifiedName.target ?: return
                val lAnnotation = o.lAnnotation ?: return
                val className = process(target.source) { className() } ?: return
                if (className == JimmerAnnotations.KotlinDto) return
                if (o.host.site != LAnnotationSite.Prop) return

                val targets = with(o.file.precompiler) { lAnnotation.targets } ?: return
                if (o.host.site !in targets) {
                    holder.registerProblem(
                        o,
                        "Annotation '@${className.fqName}' is dropped from the generated property",
                        ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                        RemoveElement(o.qualifiedName.value, o).asQuickFix(),
                    )
                    return
                }
            }
        }
    }
}

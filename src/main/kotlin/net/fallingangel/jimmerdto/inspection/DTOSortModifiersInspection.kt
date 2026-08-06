package net.fallingangel.jimmerdto.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.enums.Modifier
import net.fallingangel.jimmerdto.psi.element.DTODto
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.psi.fix.SortModifiers
import net.fallingangel.jimmerdto.psi.fix.asQuickFix

class DTOSortModifiersInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : DTOVisitor() {
            override fun visitDto(o: DTODto) {
                if (o.modifierElements.size < 2) return
                val orders = o.modifiers.map(Modifier::order)
                if (orders != orders.sorted()) {
                    val modifierElements = o.modifierElements
                    holder.registerProblem(
                        o,
                        TextRange.create(0, modifierElements.last().textRangeInParent.endOffset),
                        "Non-canonical modifier order",
                        SortModifiers(o).asQuickFix(),
                    )
                }
            }
        }
    }
}
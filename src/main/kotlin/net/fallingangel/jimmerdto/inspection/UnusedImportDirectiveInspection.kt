package net.fallingangel.jimmerdto.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.imports.DTOOptimizeImportsFix
import net.fallingangel.jimmerdto.psi.element.DTOImportStatement
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.util.file

class UnusedImportDirectiveInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : DTOVisitor() {
            override fun visitImportStatement(o: DTOImportStatement) {
                val groupedImport = o.groupedImport
                val dTOFile = o.file
                val usedTypeNames = dTOFile.usedTypeNames

                if (groupedImport != null) {
                    if (groupedImport.types.all { it.simpleName !in usedTypeNames }) {
                        holder.registerProblem(
                            o,
                            "Unused import directive",
                            ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                            DTOOptimizeImportsFix(dTOFile),
                        )
                    } else {
                        for (type in groupedImport.types) {
                            if (type.simpleName !in usedTypeNames) {
                                holder.registerProblem(
                                    type,
                                    "Unused import directive",
                                    ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                                    DTOOptimizeImportsFix(dTOFile),
                                )
                            }
                        }
                    }
                } else {
                    if (o.simpleName !in usedTypeNames) {
                        holder.registerProblem(
                            o,
                            "Unused import directive",
                            ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                            DTOOptimizeImportsFix(dTOFile),
                        )
                    }
                }
            }
        }
    }
}
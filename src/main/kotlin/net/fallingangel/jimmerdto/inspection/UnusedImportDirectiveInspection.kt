package net.fallingangel.jimmerdto.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.imports.DTOOptimizeImportsFix
import net.fallingangel.jimmerdto.psi.element.DTOImportStatement
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.psi.resolve.ImportEntry
import net.fallingangel.jimmerdto.util.equivalentTo
import net.fallingangel.jimmerdto.util.file

class UnusedImportDirectiveInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : DTOVisitor() {
            override fun visitImportStatement(o: DTOImportStatement) {
                val dtoFile = o.file
                val importEntries = o.importEntries
                val removableImportEntries = dtoFile.removableImportEntries

                fun ImportEntry.isRemovable(): Boolean {
                    return removableImportEntries.any { declaration.equivalentTo(it.declaration) }
                }

                if (importEntries.isNotEmpty() && importEntries.all(ImportEntry::isRemovable)) {
                    holder.registerProblem(
                        o,
                        "Unused import directive",
                        ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                        DTOOptimizeImportsFix(dtoFile),
                    )
                } else {
                    importEntries
                        .filter(ImportEntry::isRemovable)
                        .forEach {
                            holder.registerProblem(
                                it.declaration,
                                "Unused import directive",
                                ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                                DTOOptimizeImportsFix(dtoFile),
                            )
                        }
                }
            }
        }
    }
}

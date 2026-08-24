package net.fallingangel.jimmerdto.imports

import com.intellij.lang.ImportOptimizer
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import net.fallingangel.jimmerdto.psi.DTOFile
import net.fallingangel.jimmerdto.psi.element.DTOImportStatement
import net.fallingangel.jimmerdto.psi.element.DTOImportedType
import net.fallingangel.jimmerdto.psi.element.createImport
import net.fallingangel.jimmerdto.psi.fix.deleteWithAdjacentToken
import net.fallingangel.jimmerdto.psi.resolve.ImportEntry

class DTOImportOptimizer : ImportOptimizer {
    override fun supports(file: PsiFile) = file is DTOFile

    override fun processFile(file: PsiFile): Runnable {
        file as DTOFile
        val removableImportEntries = file.removableImportEntries

        if (removableImportEntries.isEmpty()) {
            return object : ImportOptimizer.CollectingInfoRunnable {
                override fun run() = Unit
                override fun getUserNotificationInfo() = "Unused imports not found"
            }
        }

        return Runnable {
            // 删除未使用导包节点
            removableImportEntries
                .filter { it.declaration is DTOImportStatement }
                .map(ImportEntry::declaration)
                .forEach(PsiElement::delete)
            removableImportEntries
                .filter { it.declaration is DTOImportedType }
                .map(ImportEntry::declaration)
                .forEach(PsiElement::deleteWithAdjacentToken)

            // 删除空导包组
            file.importStatements
                .filter { it.groupedImport?.types?.isEmpty() == true }
                .forEach(PsiElement::delete)

            // 展开单条导包组
            file.importStatements
                .filter { it.groupedImport?.types?.size == 1 }
                .forEach {
                    val groupedImport = it.groupedImport!!
                    val import = file.project.createImport(
                        buildString {
                            append(it.qualifiedName.value)
                            append(".")

                            val importedType = groupedImport.types.single()
                            append(importedType.type.value)

                            val alias = importedType.alias
                            if (alias != null) {
                                append(" as ")
                                append(alias.value)
                            }
                        }
                    )
                    it.replace(import)
                }
        }
    }
}

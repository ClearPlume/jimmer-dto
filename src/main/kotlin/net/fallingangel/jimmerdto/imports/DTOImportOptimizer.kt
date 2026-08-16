package net.fallingangel.jimmerdto.imports

import com.intellij.lang.ImportOptimizer
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import net.fallingangel.jimmerdto.psi.DTOFile
import net.fallingangel.jimmerdto.psi.element.createImport
import net.fallingangel.jimmerdto.psi.fix.deleteWithAdjacentToken

class DTOImportOptimizer : ImportOptimizer {
    override fun supports(file: PsiFile) = file is DTOFile

    override fun processFile(file: PsiFile): Runnable {
        file as DTOFile
        val usedTypeNames = file.usedTypeNames

        // 收集未使用导包节点
        val unusedImports = file.importStatements
            .filter { it.groupedImport == null }
            .mapNotNull { import ->
                import.takeIf { it.simpleName !in usedTypeNames }
            }
        val unusedImportTypes = file.importStatements
            .mapNotNull { it.groupedImport }
            .flatMap { group ->
                group.types
                    .mapNotNull { importedType ->
                        importedType.takeIf { it.simpleName !in usedTypeNames }
                    }
            }

        if (unusedImports.isEmpty() && unusedImportTypes.isEmpty()) {
            return object : ImportOptimizer.CollectingInfoRunnable {
                override fun run() = Unit
                override fun getUserNotificationInfo() = "Unused imports not found"
            }
        }

        return Runnable {
            // 删除未使用导包节点
            unusedImports.forEach(PsiElement::delete)
            unusedImportTypes.forEach(PsiElement::deleteWithAdjacentToken)

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

package net.fallingangel.jimmerdto.imports

import com.intellij.codeInsight.actions.OptimizeImportsProcessor
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import net.fallingangel.jimmerdto.psi.DTOFile

class DTOOptimizeImportsFix(file: DTOFile) : LocalQuickFixOnPsiElement(file) {
    override fun getText() = "Optimize imports"

    override fun getFamilyName() = text

    override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
        OptimizeImportsProcessor(project, file).run()
    }

    override fun startInWriteAction() = false
}
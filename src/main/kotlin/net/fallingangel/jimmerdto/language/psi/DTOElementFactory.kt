package net.fallingangel.jimmerdto.language.psi

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilderFactory
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiFileFactory
import net.fallingangel.jimmerdto.language.DTOFileType

object DTOElementFactory {
    fun createFile(project: Project, text: String): DTOFile {
        return PsiFileFactory.getInstance(project)
                .createFileFromText("dummy", DTOFileType.INSTANCE, text) as DTOFile
    }
}

package net.fallingangel.jimmerdto.psi

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import net.fallingangel.jimmerdto.DTOFileType

object DTOElementFactory {
    fun createFile(project: Project, text: String): DTOFile {
        return PsiFileFactory.getInstance(project)
                .createFileFromText("dummy", DTOFileType.INSTANCE, text) as DTOFile
    }
}

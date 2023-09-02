@file:JvmName("DTOPsiImplUtil")

package net.fallingangel.jimmerdto.language.psi.impl

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import net.fallingangel.jimmerdto.language.DTOFileType
import net.fallingangel.jimmerdto.language.psi.DTODto
import net.fallingangel.jimmerdto.language.psi.DTODtoName
import net.fallingangel.jimmerdto.language.psi.DTOFile

object DTOPsiImplUtil {
    @JvmStatic
    fun findDTOs(project: Project): List<DTODto> {
        val psiManager = PsiManager.getInstance(project)
        return FileTypeIndex.getFiles(DTOFileType.INSTANCE, GlobalSearchScope.allScope(project))
                .map { file ->
                    val dtoFile = psiManager.findFile(file) as DTOFile? ?: return@map emptyList()
                    PsiTreeUtil.getChildrenOfTypeAsList(dtoFile, DTODto::class.java)
                }
                .flatten()
    }

    @JvmStatic
    fun getName(element: DTODtoName): String {
        return element.identifier.text
    }

    @JvmStatic
    fun setName(element: DTODtoName, name: String): DTODtoName {
        TODO("${element.name} $name")
    }

    @JvmStatic
    fun getNameIdentifier(element: DTODtoName): PsiElement {
        return element.identifier
    }
}

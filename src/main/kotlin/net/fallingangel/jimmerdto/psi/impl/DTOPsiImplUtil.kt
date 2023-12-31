@file:JvmName("DTOPsiImplUtil")

package net.fallingangel.jimmerdto.psi.impl

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import net.fallingangel.jimmerdto.psi.DTODto
import net.fallingangel.jimmerdto.psi.DTODtoName
import net.fallingangel.jimmerdto.psi.DTOFile
import net.fallingangel.jimmerdto.psi.DTOQualifiedName

object DTOPsiImplUtil {
    /**
     * 获取element元素所在文件中的所有DTO定义
     */
    fun findDTOs(element: PsiElement): List<DTODto> {
        val dtoFile = element.containingFile as DTOFile? ?: return emptyList()
        return PsiTreeUtil.getChildrenOfTypeAsList(dtoFile, DTODto::class.java)
    }

    @JvmStatic
    fun getName(element: DTODtoName): String {
        return getNameIdentifier(element).text
    }

    @JvmStatic
    fun setName(element: DTODtoName, name: String): DTODtoName {
        TODO("${element.name} $name")
    }

    @JvmStatic
    fun getNameIdentifier(element: DTODtoName): PsiElement {
        return element.identifier
    }

    @JvmStatic
    fun getName(element: DTOQualifiedName): String {
        return getNameIdentifier(element).text
    }

    @JvmStatic
    fun setName(element: DTOQualifiedName, name: String): DTOQualifiedName {
        TODO("${element.name} $name")
    }

    @JvmStatic
    fun getNameIdentifier(element: DTOQualifiedName): PsiElement {
        return element.qualifiedNamePartList.last()
    }
}

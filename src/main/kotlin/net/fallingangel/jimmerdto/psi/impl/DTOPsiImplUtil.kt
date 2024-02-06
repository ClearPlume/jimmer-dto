@file:JvmName("DTOPsiImplUtil")

package net.fallingangel.jimmerdto.psi.impl

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import net.fallingangel.jimmerdto.psi.DTODto
import net.fallingangel.jimmerdto.psi.DTOFile
import net.fallingangel.jimmerdto.psi.DTOPositiveProp

object DTOPsiImplUtil {
    /**
     * 获取element元素所在文件中的所有DTO定义
     */
    fun findDTOs(element: PsiElement): List<DTODto> {
        val dtoFile = element.containingFile as DTOFile? ?: return emptyList()
        return PsiTreeUtil.getChildrenOfTypeAsList(dtoFile, DTODto::class.java)
    }

    /**
     * DTOPositiveProp
     */
    @JvmStatic
    fun getName(element: DTOPositiveProp): String {
        return getNameIdentifier(element).text
    }

    /**
     * DTOPositiveProp
     */
    @JvmStatic
    fun setName(element: DTOPositiveProp, name: String): DTOPositiveProp {
        TODO("${element.propName.text} $name")
    }

    /**
     * DTOPositiveProp
     */
    @JvmStatic
    fun getNameIdentifier(element: DTOPositiveProp): PsiElement {
        return element.propName
    }
}

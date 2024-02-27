@file:JvmName("DTOPsiImplUtil")

package net.fallingangel.jimmerdto.psi.impl

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import net.fallingangel.jimmerdto.psi.DTODto
import net.fallingangel.jimmerdto.psi.DTOFile

object DTOPsiImplUtil {
    /**
     * 获取element元素所在dto文件中的所有DTO定义
     */
    fun findDTOs(element: PsiElement): List<DTODto> {
        val dtoFile = element.containingFile as DTOFile? ?: return emptyList()
        return PsiTreeUtil.getChildrenOfTypeAsList(dtoFile, DTODto::class.java)
    }
}

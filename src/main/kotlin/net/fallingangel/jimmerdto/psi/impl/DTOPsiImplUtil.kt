@file:JvmName("DTOPsiImplUtil")

package net.fallingangel.jimmerdto.psi.impl

import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import net.fallingangel.jimmerdto.DTOLanguage
import net.fallingangel.jimmerdto.psi.DTODto
import net.fallingangel.jimmerdto.psi.DTOFile
import net.fallingangel.jimmerdto.psi.DTOPositiveProp
import net.fallingangel.jimmerdto.util.entityFile
import net.fallingangel.jimmerdto.util.ktClass
import net.fallingangel.jimmerdto.util.psiClass
import net.fallingangel.jimmerdto.util.virtualFile
import org.jetbrains.kotlin.psi.KtClass

object DTOPsiImplUtil {
    /**
     * 获取element元素所在dto文件对应的Java实体定义
     */
    fun findPsiClass(element: PsiElement): PsiClass {
        val project = element.project
        return element.virtualFile.entityFile(project)?.psiClass(project) ?: throw IllegalStateException("Not valid dto file")
    }

    /**
     * 获取element元素所在dto文件对应的Kotlin实体定义
     */
    fun findKtClass(element: PsiElement): KtClass {
        val project = element.project
        return element.virtualFile.entityFile(project)?.ktClass(project) ?: throw IllegalStateException("Not valid dto file")
    }

    /**
     * 获取element元素所在dto文件中的所有DTO定义
     */
    fun findDTOs(element: PsiElement): List<DTODto> {
        val dtoFile = element.containingFile as DTOFile? ?: return emptyList()
        return PsiTreeUtil.getChildrenOfTypeAsList(dtoFile, DTODto::class.java)
    }

    //////////////////////////////////////////////// DTOPositiveProp ////////////////////////////////////////////////
    @JvmStatic
    fun getName(element: DTOPositiveProp): String {
        return getNameIdentifier(element).text
    }

    @JvmStatic
    fun setName(element: DTOPositiveProp, name: String): DTOPositiveProp {
        val dtoFile = PsiFileFactory.getInstance(element.project).createFileFromText(DTOLanguage.INSTANCE, "Dummy { $name }")
        val dummy = PsiTreeUtil.getChildOfType(dtoFile, DTODto::class.java)!!
        val newNameNode = dummy.dtoBody!!.explicitPropList[0].positiveProp!!.propName.node
        element.node.replaceChild(element.propName.node, newNameNode)
        return element
    }

    @JvmStatic
    fun getNameIdentifier(element: DTOPositiveProp): PsiElement {
        return element.propName
    }
    //////////////////////////////////////////////// DTOPositiveProp ////////////////////////////////////////////////
}

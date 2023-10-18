package net.fallingangel.jimmerdto.completion.resolve.structure

import com.intellij.codeInsight.completion.CompletionUtilCore
import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.psi.DTODto
import net.fallingangel.jimmerdto.psi.DTOPositiveProp

/**
 * @param S 获取[S]类型的特定值
 * @param R 从[S]元素出发，要获取的值的类型
 */
interface Structure<S : PsiElement, R> {
    fun value(element: S): R

    val DTOPositiveProp.haveUpperProp: Boolean
        get() = parent.parent.parent.parent is DTOPositiveProp

    val DTOPositiveProp.upperProp: DTOPositiveProp
        get() = parent.parent.parent.parent as DTOPositiveProp

    private val DTOPositiveProp.name: String?
        get() = propName.text.takeIf { it != CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED }

    fun DTOPositiveProp.propPath(): List<String> {
        if (parent.parent.parent is DTODto) {
            return name?.let { listOf(it) } ?: emptyList()
        }
        return upperProp.propPath() + (name?.let { listOf(it) } ?: emptyList())
    }
}

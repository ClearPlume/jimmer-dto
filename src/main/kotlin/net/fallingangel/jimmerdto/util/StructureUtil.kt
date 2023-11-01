package net.fallingangel.jimmerdto.util

import com.intellij.codeInsight.completion.CompletionUtilCore
import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.psi.DTOAliasGroup
import net.fallingangel.jimmerdto.psi.DTODto
import net.fallingangel.jimmerdto.psi.DTOPositiveProp

val DTOPositiveProp.name: String?
    get() = propName.text.takeIf { it != CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED }

/**
 * 元素是否包含上一层级的属性级结构
 *
 * 元素：宏、属性、负属性、方法等
 * 属性级结构：flat方法、as组等
 */
val <T : PsiElement> T.haveUpper: Boolean
    get() = parent.parent is DTOAliasGroup || parent.parent.parent.parent is DTOPositiveProp

val <T : PsiElement> T.upper: PsiElement
    get() {
        return if (parent.parent is DTOAliasGroup) {
            parent.parent
        } else {
            parent.parent.parent.parent
        }
    }

fun <T : PsiElement> T.propPath(): List<String> {
    val propName = if (this is DTOPositiveProp) {
        if (name == "flat") {
            listOf(propArgs!!.valueList[0].text)
        } else {
            name?.let { listOf(it) } ?: emptyList()
        }
    } else if (this is DTOAliasGroup) {
        emptyList()
    } else {
        throw UnsupportedOperationException("Only support find path for prop, alias-group")
    }

    if (parent.parent.parent is DTODto) {
        return propName
    }
    return upper.propPath() + propName
}

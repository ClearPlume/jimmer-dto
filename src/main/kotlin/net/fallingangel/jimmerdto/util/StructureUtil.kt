package net.fallingangel.jimmerdto.util

import com.intellij.codeInsight.completion.CompletionUtilCore
import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.psi.DTODto
import net.fallingangel.jimmerdto.psi.DTOPositiveProp

val DTOPositiveProp.name: String?
    get() = propName.text.takeIf { it != CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED }

val <T : PsiElement> T.haveUpperProp: Boolean
    get() = parent.parent.parent.parent is DTOPositiveProp

val <T : PsiElement> T.upperProp: DTOPositiveProp
    get() = parent.parent.parent.parent as DTOPositiveProp

fun DTOPositiveProp.propPath(): List<String> {
    if (parent.parent.parent is DTODto) {
        return name?.let { listOf(it) } ?: emptyList()
    }
    return upperProp.propPath() + (name?.let { listOf(it) } ?: emptyList())
}

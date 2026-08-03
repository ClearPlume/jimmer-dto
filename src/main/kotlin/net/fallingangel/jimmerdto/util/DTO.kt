package net.fallingangel.jimmerdto.util

import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import com.intellij.psi.util.parentsOfType
import net.fallingangel.jimmerdto.enums.Modifier
import net.fallingangel.jimmerdto.enums.PropConfigName
import net.fallingangel.jimmerdto.psi.element.DTODto
import net.fallingangel.jimmerdto.psi.element.DTOPositiveProp

inline fun <reified T : PsiElement> PsiElement.haveParent() = parentOfType<T>() != null

inline fun <reified T : PsiElement> PsiElement.parent(withSelf: Boolean = true, predicate: T.() -> Boolean): T? {
    val parents = parentsOfType<T>(withSelf)
    return parents.find(predicate)
}

infix fun DTODto.modifiedBy(modifier: Modifier): Boolean {
    return modifier in modifiers
}

infix fun DTODto.notModifiedBy(modifier: Modifier): Boolean {
    return modifier !in modifiers
}

fun DTOPositiveProp.hasConfig(config: PropConfigName) = configs.any { it.name.text == config.text }
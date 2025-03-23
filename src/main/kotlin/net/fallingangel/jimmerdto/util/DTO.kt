package net.fallingangel.jimmerdto.util

import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import net.fallingangel.jimmerdto.completion.resolve.structure.Structure
import net.fallingangel.jimmerdto.enums.Modifier
import net.fallingangel.jimmerdto.enums.PropConfigName
import net.fallingangel.jimmerdto.psi.element.*

/**
 * 元素是否包含上一层级的属性级结构
 *
 * @receiver macro | aliasGroup | positiveProp | negativeProp | userProp
 */
val PsiElement.haveUpper: Boolean
    get() = parent.parent is DTOAliasGroup || parent.parent is DTOPropBody

/**
 * 元素上一层级的属性级结构
 *
 * @receiver macro | aliasGroup | positiveProp | negativeProp | userProp
 */
val PsiElement.upper: PsiElement
    get() = when (parent.parent) {
        is DTOAliasGroup -> {
            // aliasGroup
            parent.parent
        }

        else -> {
            // positiveProp
            parent.parent.parent
        }
    }

inline fun <reified T : PsiElement> PsiElement.haveParent() = parentOfType<T>() != null

operator fun <S : PsiElement, R, T : Structure<S, R>> S.get(type: T): R {
    return type.value(this)
}

/**
 * @receiver macro | aliasGroup | positiveProp | negativeProp | userProp
 */
fun PsiElement.propPath(): List<String> {
    val propName = when (this) {
        is DTONegativeProp -> name?.value?.let(::listOf) ?: emptyList()

        is DTOPositiveProp -> {
            val arg = arg
            if (arg == null) {
                listOf(name.value)
            } else {
                if (name.value in listOf("flat", "id")) {
                    listOf(arg.values[0].text)
                } else {
                    listOf()
                }
            }
        }

        else -> emptyList()
    }

    if (!haveUpper || parent.parent is DTODto) {
        return propName
    }
    return upper.propPath() + propName
}

infix fun DTODto.modifiedBy(modifier: Modifier): Boolean {
    return modifier in modifiers
}

infix fun DTODto.notModifiedBy(modifier: Modifier): Boolean {
    return modifier !in modifiers
}

fun DTOPositiveProp.hasConfig(config: PropConfigName) = configs.any { it.name.text == config.text }
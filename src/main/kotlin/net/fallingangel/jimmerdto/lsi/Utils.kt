package net.fallingangel.jimmerdto.lsi

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.lsi.jimmer.resolvedLClass
import org.jetbrains.kotlin.asJava.elements.KtLightElement

/**
 * 依据路径查找属性
 * @param tokens user.files.name
 */
fun LClass.findProperty(tokens: List<String>): LProperty? {
    if (tokens.isEmpty()) {
        throw IllegalStateException("Property path won't be empty")
    }
    val token = tokens.first()
    val property = allProperties.find { it.name == token } ?: return null

    if (tokens.size == 1) {
        return property
    }
    return property.findProperty(tokens.drop(1))
}

private fun LProperty.findProperty(tokens: List<String>): LProperty? {
    return when (type) {
        is LProperty.Type.Clazz -> type.clazz.findProperty(tokens)
        is LProperty.Type.Collection -> type.elementType.resolvedLClass?.findProperty(tokens)
        // 非关联属性没有子级属性，Array 是标量属性
        else -> null
    }
}

inline fun <reified T : PsiElement> PsiElement.narrow(): T {
    (this as? T)?.let { return it }
    if (this is KtLightElement<*, *>) {
        (kotlinOrigin as? T)?.let { return it }
    }
    throw IllegalArgumentException(
        "${T::class.simpleName} expected, got ${this::class.simpleName} in ${containingFile.name}"
    )
}
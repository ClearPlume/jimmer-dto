package net.fallingangel.jimmerdto.lsi

import net.fallingangel.jimmerdto.lsi.jimmer.resolvedLClass

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
package net.fallingangel.jimmerdto.lsi

import net.fallingangel.jimmerdto.exception.PropertyNotExistException

/**
 * 依据路径查找属性
 * @param tokens user.files.name
 */
fun LClass<*>.findProperty(tokens: List<String>): LProperty<*> {
    if (tokens.isEmpty()) {
        throw IllegalStateException("Property path won't be empty")
    }
    val token = tokens.first()
    val property = allProperties.find { it.name == token } ?: throw PropertyNotExistException(token)

    if (tokens.size == 1) {
        return property
    }
    return property.type.findProperty(tokens.drop(1))
}

fun LClass<*>.findPropertyOrNull(tokens: List<String>): LProperty<*>? {
    return try {
        findProperty(tokens)
    } catch (_: PropertyNotExistException) {
        null
    }
}

private fun LType.findProperty(tokens: List<String>): LProperty<*> {
    return when (this) {
        is LClass<*> -> findProperty(tokens)
        is LType.CollectionType -> elementType.findProperty(tokens)
        is LType.ArrayType -> elementType.findProperty(tokens)
        else -> throw IllegalStateException("Unsupported operation in $name")
    }
}
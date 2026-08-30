package net.fallingangel.jimmerdto.lsi.annotation

import net.fallingangel.jimmerdto.lsi.LName

interface LAnnotationOwner {
    val annotations: List<LAnnotation>

    fun findAnnotation(name: LName): LAnnotation? {
        return annotations.find { it.lName == name }
    }

    fun hasAnnotation(vararg annotationClass: LName): Boolean {
        return annotations.any { it.lName in annotationClass }
    }

    fun hasAnnotationBySimple(vararg simpleAnnotation: String): Boolean {
        return annotations.any { it.name in simpleAnnotation }
    }

    fun annotationsToString(visited: MutableSet<String>): String {
        return annotations.joinToString(prefix = "[", postfix = "]") { it.toDebugString(visited) }
    }
}

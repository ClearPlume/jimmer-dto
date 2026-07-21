package net.fallingangel.jimmerdto.lsi.annotation

import kotlin.reflect.KClass

fun LAnnotationOwner.hasAnnotation(annotationClass: KClass<out Annotation>): Boolean {
    return annotations.any { it.canonicalName == annotationClass.qualifiedName }
}

fun LAnnotationOwner.hasAnnotation(vararg annotationClass: KClass<out Annotation>): Boolean {
    return annotations.any { it.canonicalName in annotationClass.map(KClass<*>::qualifiedName) }
}

fun LAnnotationOwner.hasAnnotationBySimple(vararg simpleAnnotation: String): Boolean {
    return annotations.any { it.name in simpleAnnotation }
}

fun LAnnotationOwner.annotationsToString(visited: MutableSet<String>): String {
    return annotations.joinToString(prefix = "[", postfix = "]") { it.toDebugString(visited) }
}

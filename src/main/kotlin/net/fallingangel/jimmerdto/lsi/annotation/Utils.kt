package net.fallingangel.jimmerdto.lsi.annotation

import net.fallingangel.jimmerdto.lsi.param.paramsToString
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

fun LAnnotationOwner.hasExactlyOneAnnotation(vararg annotationClasses: KClass<out Annotation>): Boolean {
    val count = annotationClasses.count { annotationClass ->
        annotations.any { it.canonicalName == annotationClass.qualifiedName }
    }
    return count == 1
}

fun LAnnotationOwner.annotationsToString(visited: MutableSet<String>): String {
    return annotations.joinToString(prefix = "[", postfix = "]") { it.toDebugString(visited) }
}

fun LAnnotation<*>.toDebugString(visited: MutableSet<String>): String {
    val paramsStr = paramsToString(visited)
    return "LAnnotation(name=$name, params=$paramsStr)"
}
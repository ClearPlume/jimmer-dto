package net.fallingangel.jimmerdto.lsi.annotation

import kotlin.reflect.KClass
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotation.Param.Value as ParamValue

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

infix fun ParamValue?.eq(constant: Enum<*>): Boolean {
    if (this !is ParamValue.Enum) return false
    val javaClass = constant.declaringJavaClass
    return canonicalName == javaClass.canonicalName && constantName == constant.name
}

infix fun ParamValue?.eq(expected: String): Boolean {
    return this is ParamValue.Scalar && value == expected
}

infix fun ParamValue?.eq(expected: Boolean): Boolean {
    return this is ParamValue.Scalar && value == expected
}
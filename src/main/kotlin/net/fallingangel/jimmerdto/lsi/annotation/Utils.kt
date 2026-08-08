package net.fallingangel.jimmerdto.lsi.annotation

import org.jetbrains.kotlin.name.ClassId
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotation.Param.Value as ParamValue

fun LAnnotationOwner.hasAnnotation(annotationClass: ClassId): Boolean {
    return annotations.any { it.canonicalName == annotationClass.asFqNameString() }
}

fun LAnnotationOwner.hasAnnotation(vararg annotationClass: ClassId): Boolean {
    return annotations.any { it.canonicalName in annotationClass.map(ClassId::asFqNameString) }
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
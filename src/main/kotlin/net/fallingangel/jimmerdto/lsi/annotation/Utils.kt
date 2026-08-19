package net.fallingangel.jimmerdto.lsi.annotation

import net.fallingangel.jimmerdto.lsi.LName
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotation.Param.Value as ParamValue

fun LAnnotationOwner.hasAnnotation(annotationClass: LName): Boolean {
    return annotations.any { it.fqName == annotationClass.fqName }
}

fun LAnnotationOwner.hasAnnotation(vararg annotationClass: LName): Boolean {
    return annotations.any { it.fqName in annotationClass.map(LName::fqName) }
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
    return typeName == javaClass.canonicalName && constantName == constant.name
}

infix fun ParamValue?.eq(expected: String): Boolean {
    return this is ParamValue.Scalar && value == expected
}

infix fun ParamValue?.eq(expected: Boolean): Boolean {
    return this is ParamValue.Scalar && value == expected
}

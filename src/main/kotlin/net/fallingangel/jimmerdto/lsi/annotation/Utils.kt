package net.fallingangel.jimmerdto.lsi.annotation

import net.fallingangel.jimmerdto.lsi.annotation.LAnnotation.Param.Value as ParamValue

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

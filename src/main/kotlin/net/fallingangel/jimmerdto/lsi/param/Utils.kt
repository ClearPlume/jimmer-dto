package net.fallingangel.jimmerdto.lsi.param

fun LParamOwner.paramsToString(visited: MutableSet<String>): String {
    return params.joinToString(prefix = "[", postfix = "]") { it.toDebugString(visited) }
}

fun LParam<*>.toDebugString(visited: MutableSet<String>): String {
    return "LParam(name=$name, type=${type.toDebugString(visited)})"
}
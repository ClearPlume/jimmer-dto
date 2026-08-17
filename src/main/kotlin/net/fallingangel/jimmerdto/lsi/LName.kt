package net.fallingangel.jimmerdto.lsi

data class LName(val pkg: String, val nesting: List<String>, val name: String) {
    constructor(name: String) : this("", name)
    constructor(pkg: String, name: String) : this(pkg, emptyList(), name)

    val fqName = buildString {
        if (pkg.isNotEmpty()) {
            append(pkg)
            append('.')
        }
        if (nesting.isNotEmpty()) {
            append(nesting.joinToString("."))
            append('.')
        }
        append(name)
    }

    override fun toString(): String {
        return fqName
    }
}

package net.fallingangel.jimmerdto.lsi

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

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

    fun toClassId(): ClassId {
        return ClassId(FqName(pkg), FqName((nesting + name).joinToString(".")), false)
    }

    override fun toString(): String {
        return fqName
    }

    companion object {
        fun fromFqn(fqName: String): LName {
            return if ('.' in fqName) {
                val pkg = fqName.substringBeforeLast('.')
                val name = fqName.substringAfterLast('.')
                LName(pkg, name)
            } else {
                LName(fqName)
            }
        }
    }
}

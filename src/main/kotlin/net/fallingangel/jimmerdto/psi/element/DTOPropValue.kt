package net.fallingangel.jimmerdto.psi.element

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.enums.SimplePropType
import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTOPropValue : DTOElement {
    val boolean: PsiElement?

    val character: PsiElement?

    val sqlString: PsiElement?

    val integer: PsiElement?

    val float: PsiElement?

    val kind: Kind
        get() = when {
            boolean != null -> Kind.Boolean
            character != null -> Kind.Character
            sqlString != null -> Kind.SqlString
            integer != null -> Kind.Integer
            float != null -> Kind.Float
            else -> grammarMismatch()
        }

    enum class Kind(val literalName: String, val accepted: SimplePropType.Family) {
        Boolean("boolean", SimplePropType.Family.Boolean),
        Character("char", SimplePropType.Family.String),
        SqlString("string", SimplePropType.Family.String),
        Integer("integer", SimplePropType.Family.Integer),
        Float("float", SimplePropType.Family.Float),
    }
}
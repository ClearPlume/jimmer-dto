package net.fallingangel.jimmerdto.enums

import net.fallingangel.jimmerdto.psi.DTODto

enum class Modifier(val value: String) {
    INPUT("input"), SPECIFICATION("specification"), ABSTRACT("abstract"), UNSAFE("unsafe"), DYNAMIC("dynamic")
}

infix fun DTODto.modifiedBy(modifier: Modifier): Boolean {
    return modifier.value in this.dtoModifierList.map { it.text }
}

infix fun DTODto.notModifiedBy(modifier: Modifier): Boolean {
    return !modifiedBy(modifier)
}

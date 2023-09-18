package net.fallingangel.jimmerdto.enums

import net.fallingangel.jimmerdto.psi.DTODto

enum class Modifier(vararg val value: String) {
    INPUT("input"), INPUT_ONLY("input-only", "inputOnly"), ABSTRACT("abstract")
}

infix fun DTODto.modifiedBy(modifier: Modifier): Boolean {
    return dtoModifierList.any { it.text in modifier.value }
}

infix fun DTODto.notModifiedBy(modifier: Modifier): Boolean {
    return !modifiedBy(modifier)
}

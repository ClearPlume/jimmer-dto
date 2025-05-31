package net.fallingangel.jimmerdto.psi.element

import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTODtoBody : DTOElement {
    val macros: List<DTOMacro>

    val aliasGroups: List<DTOAliasGroup>

    val positiveProps: List<DTOPositiveProp>

    val negativeProps: List<DTONegativeProp>

    val userProps: List<DTOUserProp>

    val existedProps: List<String>
        get() {
            val aliasProps = aliasGroups.flatMap { it.positiveProps.map { prop -> prop.alias?.value ?: prop.name.value } }
            val positiveProps = positiveProps.map { it.alias?.value ?: it.name.value }
            val userProps = userProps.map { it.name.value }
            return aliasProps + userProps + positiveProps
        }
}
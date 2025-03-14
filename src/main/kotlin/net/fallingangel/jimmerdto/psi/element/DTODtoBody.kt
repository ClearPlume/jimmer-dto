package net.fallingangel.jimmerdto.psi.element

import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTODtoBody : DTOElement {
    val macros: List<DTOMacro>

    val aliasGroups: List<DTOAliasGroup>

    val positiveProps: List<DTOPositiveProp>

    val negativeProps: List<DTONegativeProp>

    val userProps: List<DTOUserProp>
}
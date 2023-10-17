package net.fallingangel.jimmerdto.completion.resolve

import net.fallingangel.jimmerdto.completion.resolve.structure.*

object StructureType {
    val RelationProperties = RelationProperties()
    val NegativeRelationProperties = NegativeRelationProperties()
    val DtoProperties = DtoProperties()
    val NegativeDtoProperties = NegativeDtoProperties()

    val MacroTypes = MacroTypes()
    val RelationMacroTypes = RelationMacroTypes()

    val PropArgs = PropArgs()
    val RelationPropArgs = RelationPropArgs()

    val DtoSupers = DtoSupers()
    val DtoModifiers = DtoModifiers()

    val EnumInstances = EnumInstances()
    val RelationEnumInstances = RelationEnumInstances()
}
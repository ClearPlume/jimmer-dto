package net.fallingangel.jimmerdto.completion.resolve

import net.fallingangel.jimmerdto.completion.resolve.structure.*

object StructureType {
    val DtoSupers = DtoSupers()
    val DtoModifiers = DtoModifiers()

    // properties
    val DtoProperties = DtoProperties()
    val NegativeDtoProperties = NegativeDtoProperties()
    val RelationProperties = RelationProperties()
    val NegativeRelationProperties = NegativeRelationProperties()

    // macros
    val MacroTypes = MacroTypes()
    val RelationMacroTypes = RelationMacroTypes()

    // function args
    val FunctionArgs = FunctionArgs()
    val RelationFunctionArgs = RelationFunctionArgs()

    // enums
    val EnumInstances = EnumInstances()
    val RelationEnumInstances = RelationEnumInstances()
}
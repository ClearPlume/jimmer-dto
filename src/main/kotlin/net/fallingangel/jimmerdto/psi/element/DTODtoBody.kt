package net.fallingangel.jimmerdto.psi.element

import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTODtoBody : DTOElement {
    val macros: List<DTOMacro>

    val aliasGroups: List<DTOAliasGroup>

    val positiveProps: List<DTOPositiveProp>

    val negativeProps: List<DTONegativeProp>

    val userProps: List<DTOUserProp>

    val availableProps: List<String>
        get() = macros.flatMap(DTOMacro::carriedProps)

    /**
     * 属性名称 to 别名/null
     *
     * ```
     * interface User {
     *     val name: Int
     *     val project: Project
     *     val friends: List<User>
     * }
     * ```
     * ```
     * in dto:
     * project { ... }
     *
     * in map:
     * project to (1 to null)
     * ```
     * ```
     * in dto:
     * id(project)
     *
     * in map:
     * project to (1 to projectId)
     * ```
     * ```
     * in dto:
     * #allReferences
     * #allScalars
     *
     * in map:
     * project to (1 to projectId)
     * name to (1 to null)
     * ```
     * ```
     * in dto:
     * name
     * as($ -> file) {
     *     name
     * }
     * s: String
     * id(parent)
     * id(childFiles) as childFileIds
     *
     * in map:
     * name to (2 to null)
     * s to (1 to null)
     * parent to (1 to parentId)
     * childFiles to (1 to childFileIds)
     * ```
     */
    val existedProp: Map<String, Pair<Int, String?>>
        get() {
            val aliasProps = aliasGroups.flatMap { alias ->
                val pos: List<Pair<String, String?>> = alias.positiveProps
                        .map {
                            val name = it.name.value
                            name to alias.apply(name)
                        }
                val mac: List<Pair<String, String?>> = alias.macros
                        .filter(DTOMacro::isScalar)
                        .flatMap { macro -> macro.carriedProps.map { it to alias.apply(it) } }
                pos + mac
            }
            val idFunctionProps = positiveProps
                    .filter { it.arg != null && it.name.value == "id" }
                    .mapNotNull { prop ->
                        val arg = prop.arg!!
                        val argName = arg.values.first().name ?: return@mapNotNull null
                        val alias = prop.alias
                        if (alias == null) {
                            argName to "${argName}Id"
                        } else {
                            argName to alias.value
                        }
                    }

            val functionProps = positiveProps
                    .filter { it.arg != null && it.name.value != "id" }
                    .mapNotNull { it.arg }
                    .flatMap { it.values.map { v -> v.text to null } }
            val positiveProps = positiveProps.filter { it.arg == null }
                    .map { it.name.value to it.alias?.value }
            val userProps = userProps.map { it.name.value to null }

            return (aliasProps + idFunctionProps + functionProps + positiveProps + userProps)
                    .groupingBy { it.first }
                    .fold({ _, element -> 0 to element.second }) { _, acc, element ->
                        acc.first + 1 to (acc.second ?: element.second)
                    }
        }
}
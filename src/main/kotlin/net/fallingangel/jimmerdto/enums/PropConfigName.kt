package net.fallingangel.jimmerdto.enums

import net.fallingangel.jimmerdto.lsi.LProperty
import net.fallingangel.jimmerdto.lsi.jimmer.isEntityAssociation
import net.fallingangel.jimmerdto.lsi.jimmer.isList
import net.fallingangel.jimmerdto.lsi.jimmer.isRecursive
import net.fallingangel.jimmerdto.lsi.jimmer.isReference
import net.fallingangel.jimmerdto.psi.element.DTOPositiveProp
import net.fallingangel.jimmerdto.psi.element.DTOPropConfig
import kotlin.enums.enumEntries

enum class PropConfigName(val text: String) {
    Where("!where") {
        override fun variantViolations(prop: DTOPositiveProp, property: LProperty): List<String> {
            return buildList {
                if (!property.isEntityAssociation) {
                    add("Cannot specify '$text' when the property is not association")
                }

                if (property.isReference && !property.nullable) {
                    add("Cannot specify '$text' when the property is non-null reference")
                }
            }
        }
    },
    OrderBy("!orderBy") {
        override fun variantViolations(prop: DTOPositiveProp, property: LProperty): List<String> {
            return buildList {
                if (!property.isEntityAssociation || !property.isList) {
                    add("Cannot specify '$text' when the property is not associated list")
                }
            }
        }
    },
    Filter("!filter") {
        override fun variantViolations(prop: DTOPositiveProp, property: LProperty): List<String> {
            return buildList {
                if (!property.isEntityAssociation || !property.isList) {
                    add("Cannot specify '$text' when the property is not associated list")
                }
            }
        }
    },
    Recursion("!recursion") {
        override fun variantViolations(prop: DTOPositiveProp, property: LProperty): List<String> {
            return buildList {
                if (!prop.isRecursive || !property.isRecursive) {
                    add("'$text' can only be applied for recursive property")
                }
            }
        }
    },
    FetchType("!fetchType") {
        override fun variantViolations(prop: DTOPositiveProp, property: LProperty): List<String> {
            return buildList {
                if (!property.isEntityAssociation || property.isList) {
                    add("Cannot specify '$text' when the property is not associated reference")
                }
            }
        }
    },
    Limit("!limit") {
        override fun variantViolations(prop: DTOPositiveProp, property: LProperty): List<String> {
            return buildList {
                if (!property.isEntityAssociation || !property.isList) {
                    add("Cannot specify '$text' when the property is not associated list")
                }
            }
        }
    },
    Batch("!batch") {
        override fun variantViolations(prop: DTOPositiveProp, property: LProperty): List<String> {
            return buildList {
                if (!property.isEntityAssociation || !property.isList) {
                    add("Cannot specify '$text' when the property is not associated list")
                }
            }
        }
    },
    Depth("!depth") {
        override fun variantViolations(prop: DTOPositiveProp, property: LProperty): List<String> {
            return buildList {
                if (!prop.isRecursive || !property.isRecursive) {
                    add("'$text' can only be applied for recursive property")
                }
            }
        }
    };

    open fun violations(config: DTOPropConfig, prop: DTOPositiveProp, property: LProperty): List<String> {
        return variantViolations(prop, property) + buildList {
            val conflicts = exclusive[text].orEmpty()
            prop.configs
                .filter { it !== config }
                .firstOrNull { it.name.text in conflicts }
                ?.let {
                    add("Cannot specify '$text' when '${it.name.text}' exists")
                }
        }
    }

    abstract fun variantViolations(prop: DTOPositiveProp, property: LProperty): List<String>

    companion object {
        val availableNames = entries.map(PropConfigName::text)

        val exclusive = listOf(Where to Filter, OrderBy to Filter, Recursion to Depth)
            .flatMap { (a, b) -> listOf(a.text to b.text, b.text to a.text) }
            .groupBy({ it.first }, { it.second })

        fun fromText(name: String): PropConfigName? {
            return enumEntries<PropConfigName>().find { it.text == name }
        }
    }
}

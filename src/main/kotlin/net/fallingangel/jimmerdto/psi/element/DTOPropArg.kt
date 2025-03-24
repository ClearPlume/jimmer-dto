package net.fallingangel.jimmerdto.psi.element

import com.intellij.psi.util.parentOfType
import net.fallingangel.jimmerdto.enums.Function
import net.fallingangel.jimmerdto.enums.Modifier
import net.fallingangel.jimmerdto.enums.SpecFunction
import net.fallingangel.jimmerdto.lsi.LProperty
import net.fallingangel.jimmerdto.psi.mixin.DTOElement
import net.fallingangel.jimmerdto.structure.ArgType
import net.fallingangel.jimmerdto.structure.ArgType.Companion.or
import net.fallingangel.jimmerdto.util.modifiedBy

interface DTOPropArg : DTOElement {
    val values: List<DTOValue>

    val args: List<LProperty<*>>
        get() {
            val dto = parentOfType<DTODto>() ?: return emptyList()
            val prop = parent as DTOPositiveProp
            val functionName = prop.name.value
            val properties = prop.allSiblings()

            return when (functionName) {
                Function.Id.expression -> {
                    properties.filter { Function.Id.argType.test(it) }
                }

                Function.Flat.expression -> {
                    if (dto modifiedBy Modifier.Specification) {
                        properties.filter { ArgType.ListAssociation.test(it) }
                    } else {
                        properties.filter { ArgType.SingleAssociation.or(ArgType.Embeddable).test(it) }
                    }
                }

                in SpecFunction.values().map { it.expression } -> {
                    properties
                            .filter { property ->
                                val specFunction = SpecFunction.values().first { it.expression == functionName }
                                specFunction.argType.test(property)
                            }
                }

                else -> {
                    throw IllegalStateException("Illegal function $functionName")
                }
            }
        }
}
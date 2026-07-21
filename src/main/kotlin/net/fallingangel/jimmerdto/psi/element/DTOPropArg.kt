package net.fallingangel.jimmerdto.psi.element

import com.intellij.psi.util.parentOfType
import net.fallingangel.jimmerdto.enums.Function
import net.fallingangel.jimmerdto.lsi.LProperty
import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTOPropArg : DTOElement {
    val values: List<DTOValue>

    val isEmpty: Boolean
        get() = values.isEmpty() || values.size == 1 && values.first().text == ""

    val args: List<LProperty>?
        get() {
            val prop = parent as DTOPositiveProp
            val function = Function.entries.find { it.expression == prop.name.value } ?: return null
            val dto = parentOfType<DTODto>() ?: return null
            val argConstraint = function.argConstraint ?: return null
            val properties = prop.containingLClass?.allProperties ?: return null
            return properties.filter { argConstraint(dto).test(it) }
        }
}
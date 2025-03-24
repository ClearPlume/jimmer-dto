package net.fallingangel.jimmerdto.psi.element

import net.fallingangel.jimmerdto.lsi.LClass
import net.fallingangel.jimmerdto.lsi.LProperty
import net.fallingangel.jimmerdto.lsi.findPropertyOrNull
import net.fallingangel.jimmerdto.psi.mixin.DTOElement
import net.fallingangel.jimmerdto.util.file
import net.fallingangel.jimmerdto.util.propPath

interface DTONegativeProp : DTOElement {
    val name: DTOPropName?

    val property: LProperty<*>?
        get() = file.clazz.findPropertyOrNull(propPath())

    fun allSiblings(withSelf: Boolean = false): List<LProperty<*>> {
        val propPath = propPath().dropLast(if (withSelf) 0 else 1)
        return if (propPath.isEmpty()) {
            file.clazz.allProperties
        } else {
            val parentClazz = file.clazz.findPropertyOrNull(propPath)?.actualType as? LClass<*> ?: return emptyList()
            parentClazz.allProperties
        }
    }
}
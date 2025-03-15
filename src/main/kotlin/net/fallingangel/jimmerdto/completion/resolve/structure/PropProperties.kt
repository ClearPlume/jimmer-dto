package net.fallingangel.jimmerdto.completion.resolve.structure

import net.fallingangel.jimmerdto.exception.PropertyNotExistException
import net.fallingangel.jimmerdto.lsi.LProperty
import net.fallingangel.jimmerdto.psi.element.DTOPositiveProp
import net.fallingangel.jimmerdto.util.file
import net.fallingangel.jimmerdto.util.propPath

class PropProperties : Structure<DTOPositiveProp, List<LProperty<*>>> {
    /**
     * @param element DTO或关联属性中的属性元素
     *
     * @return 属性对应的实体中的所有属性列表
     */
    override fun value(element: DTOPositiveProp): List<LProperty<*>> {
        val propPath = element.propPath().dropLast(1)
        return try {
            element.file.clazz.walk(propPath).allProperties
        } catch (_: PropertyNotExistException) {
            println("Property not found: $propPath")
            emptyList()
        }
    }
}
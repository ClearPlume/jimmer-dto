package net.fallingangel.jimmerdto.completion.resolve.structure

import net.fallingangel.jimmerdto.lsi.LType
import net.fallingangel.jimmerdto.psi.element.DTOPositiveProp
import net.fallingangel.jimmerdto.util.file
import net.fallingangel.jimmerdto.util.propPath

class EnumValues : Structure<DTOPositiveProp, List<String>> {
    /**
     * @param element DTO或关联属性中的枚举属性元素
     *
     * @return 枚举可用实例
     */
    override fun value(element: DTOPositiveProp): List<String> {
        val propPath = element.propPath()
        val property = element.file.clazz.propertyOrNull(propPath) ?: return listOf()
        val enumType = property.type as LType.EnumType<*, *>
        return enumType.values.keys.toList()
    }
}
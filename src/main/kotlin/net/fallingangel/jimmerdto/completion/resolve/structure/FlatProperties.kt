package net.fallingangel.jimmerdto.completion.resolve.structure

import net.fallingangel.jimmerdto.psi.DTOPositiveProp
import net.fallingangel.jimmerdto.psi.DTOValue
import net.fallingangel.jimmerdto.structure.Property
import net.fallingangel.jimmerdto.util.*

class FlatProperties : Structure<DTOValue, List<Property>> {
    /**
     * @param element DTO或关联属性中的flat元素
     *
     * @return Flat方法参数对应的实体中的所有属性列表
     */
    override fun value(element: DTOValue): List<Property> {
        val prop = element.parent.parent as DTOPositiveProp
        val properties = if (prop.haveUpper) {
            element.virtualFile.properties(element.project, prop.propPath())
        } else {
            element.virtualFile.properties(element.project, listOf(element.text))
        }
        return properties
    }
}
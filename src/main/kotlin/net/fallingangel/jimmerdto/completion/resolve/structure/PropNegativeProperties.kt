package net.fallingangel.jimmerdto.completion.resolve.structure

import net.fallingangel.jimmerdto.psi.DTONegativeProp
import net.fallingangel.jimmerdto.structure.Property
import net.fallingangel.jimmerdto.util.*

class PropNegativeProperties : Structure<DTONegativeProp, List<Property>> {
    /**
     * @param element DTO或关联属性中的负属性元素
     *
     * @return 属性对应的实体中的所有属性列表
     */
    override fun value(element: DTONegativeProp): List<Property> {
        return if (element.haveUpperProp) {
            element.virtualFile.properties(element.project, element.upperProp.propPath())
        } else {
            element.virtualFile.properties(element.project)
        }
    }
}
package net.fallingangel.jimmerdto.completion.resolve.structure

import net.fallingangel.jimmerdto.psi.DTOPositiveProp
import net.fallingangel.jimmerdto.psi.DTOPropName
import net.fallingangel.jimmerdto.structure.Property
import net.fallingangel.jimmerdto.util.*

class PropProperties : Structure<DTOPropName, List<Property>> {
    /**
     * @param element DTO或关联属性中的属性元素
     *
     * @return 属性对应的实体中的所有属性列表
     */
    override fun value(element: DTOPropName): List<Property> {
        val prop = element.parent as DTOPositiveProp
        return if (prop.haveUpperProp) {
            element.virtualFile.properties(element.project, prop.upperProp.propPath())
        } else {
            element.virtualFile.properties(element.project)
        }
    }
}
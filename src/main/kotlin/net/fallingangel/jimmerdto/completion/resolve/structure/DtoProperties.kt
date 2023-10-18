package net.fallingangel.jimmerdto.completion.resolve.structure

import net.fallingangel.jimmerdto.psi.DTOPositiveProp
import net.fallingangel.jimmerdto.psi.DTOPropName
import net.fallingangel.jimmerdto.structure.Property
import net.fallingangel.jimmerdto.util.properties
import net.fallingangel.jimmerdto.util.virtualFile

class DtoProperties : Structure<DTOPropName, List<Property>> {
    /**
     * @param element DTO中的属性元素
     *
     * @return DTO对应的实体中的所有属性列表
     */
    override fun value(element: DTOPropName): List<Property> {
        val parentProp = element.parent as DTOPositiveProp
        val propPath = if (parentProp.haveUpperProp) {
            parentProp.upperProp.propPath()
        } else {
            emptyList()
        }
        return element.virtualFile.properties(element.project, propPath)
    }
}
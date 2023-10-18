package net.fallingangel.jimmerdto.completion.resolve.structure

import net.fallingangel.jimmerdto.psi.DTOPositiveProp
import net.fallingangel.jimmerdto.psi.DTOPropName
import net.fallingangel.jimmerdto.structure.Property
import net.fallingangel.jimmerdto.util.properties
import net.fallingangel.jimmerdto.util.virtualFile

class RelationProperties : Structure<DTOPropName, List<Property>> {
    /**
     * @param element 关联关系中的属性元素
     *
     * @return 获取[element]所在的关联关系属性，并进一步获取到的关联关系对应的实体中的属性列表
     */
    override fun value(element: DTOPropName): List<Property> {
        val parentProp = element.parent.parent.parent.parent.parent as DTOPositiveProp
        return element.virtualFile.properties(element.project, parentProp.propPath())
    }
}
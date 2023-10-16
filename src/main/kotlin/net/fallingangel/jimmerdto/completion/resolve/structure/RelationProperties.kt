package net.fallingangel.jimmerdto.completion.resolve.structure

import net.fallingangel.jimmerdto.psi.DTODtoBody
import net.fallingangel.jimmerdto.psi.DTOPositiveProp
import net.fallingangel.jimmerdto.structure.Property
import net.fallingangel.jimmerdto.util.properties
import net.fallingangel.jimmerdto.util.virtualFile

class RelationProperties : Structure<DTODtoBody, List<Property>> {
    /**
     * @param element 关联关系中的属性元素
     *
     * @return 获取[element]所在的关联关系属性，并进一步获取到的关联关系对应的实体中的属性列表
     */
    override fun value(element: DTODtoBody): List<Property> {
        val propName = (element.parent.parent as DTOPositiveProp).propName.text
        return element.virtualFile.properties(element.project, propName)
    }
}
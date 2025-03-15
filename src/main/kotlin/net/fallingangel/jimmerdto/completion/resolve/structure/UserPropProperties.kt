package net.fallingangel.jimmerdto.completion.resolve.structure

import net.fallingangel.jimmerdto.lsi.LProperty
import net.fallingangel.jimmerdto.psi.element.DTOUserProp
import net.fallingangel.jimmerdto.util.file
import net.fallingangel.jimmerdto.util.propPath

class UserPropProperties : Structure<DTOUserProp, List<LProperty<*>>> {
    /**
     * @param element 用户属性元素
     *
     * @return 属性对应的实体中的所有属性列表
     */
    override fun value(element: DTOUserProp): List<LProperty<*>> {
        return element.file.clazz.walk(element.propPath()).properties
    }
}
package net.fallingangel.jimmerdto.completion.resolve.structure

import net.fallingangel.jimmerdto.lsi.LClass
import net.fallingangel.jimmerdto.psi.element.DTOMacro
import net.fallingangel.jimmerdto.util.*

class MacroTypes : Structure<DTOMacro, List<String>> {
    /**
     * @param element Dto宏的参数元素
     *
     * @return 宏的可用参数类型列表
     */
    override fun value(element: DTOMacro): List<String> {
        val propPath = element.propPath()
        val clazz = element.file.clazz.walk(propPath)
        return clazz.allParents.map(LClass<*>::name) + clazz.name + "this"
    }
}
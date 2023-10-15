package net.fallingangel.jimmerdto.completion.resolve.structure

import net.fallingangel.jimmerdto.psi.DTOMacroArgs
import net.fallingangel.jimmerdto.util.supers
import net.fallingangel.jimmerdto.util.virtualFile

class MacroTypes : Structure<DTOMacroArgs, List<String>> {
    /**
     * @param element Dto宏的参数元素
     *
     * @return 宏的可用参数类型列表
     */
    override fun value(element: DTOMacroArgs): List<String> {
        val dtoFile = element.virtualFile
        val project = element.project
        return dtoFile.supers(project) + dtoFile.name.substringBeforeLast('.')
    }
}
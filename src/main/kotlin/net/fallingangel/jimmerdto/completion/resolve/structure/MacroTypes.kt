package net.fallingangel.jimmerdto.completion.resolve.structure

import net.fallingangel.jimmerdto.psi.DTOMacroArgs
import net.fallingangel.jimmerdto.util.*
import net.fallingangel.jimmerdto.util.haveUpperProp
import net.fallingangel.jimmerdto.util.upperProp

class MacroTypes : Structure<DTOMacroArgs, List<String>> {
    /**
     * @param element Dto宏的参数元素
     *
     * @return 宏的可用参数类型列表
     */
    override fun value(element: DTOMacroArgs): List<String> {
        val project = element.project
        val entityFile = element.virtualFile.entityFile(project) ?: return emptyList()

        val propPath = if (element.parent.haveUpperProp) {
            element.parent.upperProp.propPath()
        } else {
            emptyList()
        }
        val propDtoFile = if (entityFile.isJavaOrKotlin) {
            entityFile.psiClass(project, propPath)?.virtualFile ?: return emptyList()
        } else {
            entityFile.ktClass(project, propPath)?.virtualFile ?: return emptyList()
        }
        return propDtoFile.supers(project) + propDtoFile.name.substringBeforeLast('.')
    }
}
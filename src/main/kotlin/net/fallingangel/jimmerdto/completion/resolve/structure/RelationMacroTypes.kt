package net.fallingangel.jimmerdto.completion.resolve.structure

import net.fallingangel.jimmerdto.psi.DTOMacroArgs
import net.fallingangel.jimmerdto.psi.DTOPositiveProp
import net.fallingangel.jimmerdto.util.*

class RelationMacroTypes : Structure<DTOMacroArgs, List<String>> {
    /**
     * @param element 关联属性宏的参数元素
     *
     * @return 宏的可用参数类型列表
     */
    override fun value(element: DTOMacroArgs): List<String> {
        val project = element.project
        val entityFile = element.virtualFile.entityFile(project) ?: return emptyList()
        val propName = (element.parent.parent.parent.parent.parent as DTOPositiveProp).propName.text
        val propDtoFile = if (entityFile.isJavaOrKotlin) {
            entityFile.psiClass(project, propName)?.virtualFile ?: return emptyList()
        } else {
            entityFile.ktClass(project, propName)?.virtualFile ?: return emptyList()
        }
        return propDtoFile.supers(project) + propDtoFile.name.substringBeforeLast('.')
    }
}
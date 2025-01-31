package net.fallingangel.jimmerdto.completion.resolve.structure

import net.fallingangel.jimmerdto.enums.Language
import net.fallingangel.jimmerdto.psi.DTOMacroArgs
import net.fallingangel.jimmerdto.util.*

class MacroTypes : Structure<DTOMacroArgs, List<String>> {
    /**
     * @param element Dto宏的参数元素
     *
     * @return 宏的可用参数类型列表
     */
    override fun value(element: DTOMacroArgs): List<String> {
        val project = element.project
        val entityFile = element.virtualFile.entityFile(project) ?: return emptyList()

        val propPath = if (element.parent.haveUpper) {
            element.parent.upper.propPath()
        } else {
            emptyList()
        }
        val propClassFile = when (entityFile.language) {
            Language.Java -> {
                entityFile.psiClass(project, propPath)?.virtualFile ?: return emptyList()
            }

            Language.Kotlin -> {
                entityFile.ktClass(project, propPath)?.virtualFile ?: return emptyList()
            }
        }
        return propClassFile.supers(project) + propClassFile.name.substringBeforeLast('.') + "this"
    }
}
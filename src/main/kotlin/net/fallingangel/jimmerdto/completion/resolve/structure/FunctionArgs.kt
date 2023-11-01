package net.fallingangel.jimmerdto.completion.resolve.structure

import net.fallingangel.jimmerdto.psi.DTOPositiveProp
import net.fallingangel.jimmerdto.psi.DTOPropArgs
import net.fallingangel.jimmerdto.structure.Property
import net.fallingangel.jimmerdto.structure.RelationType
import net.fallingangel.jimmerdto.util.*

class FunctionArgs : Structure<DTOPropArgs, List<Property>> {
    /**
     * @param element 方法参数元素
     *
     * @return 可用方法参数
     */
    override fun value(element: DTOPropArgs): List<Property> {
        val prop = element.parent as DTOPositiveProp
        val propPath = if (prop.haveUpper) {
            prop.upper.propPath()
        } else {
            emptyList()
        }
        return element.virtualFile
                .properties(element.project, propPath)
                .filter { property ->
                    property.annotations
                            .map { annotation ->
                                annotation.substringAfterLast('.')
                            }
                            .any { annotationName ->
                                annotationName in RelationType.values().map { it.name }
                            }
                }
    }
}
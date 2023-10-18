package net.fallingangel.jimmerdto.completion.resolve.structure

import net.fallingangel.jimmerdto.psi.DTOPositiveProp
import net.fallingangel.jimmerdto.psi.DTOPropArgs
import net.fallingangel.jimmerdto.structure.Property
import net.fallingangel.jimmerdto.structure.RelationType
import net.fallingangel.jimmerdto.util.properties
import net.fallingangel.jimmerdto.util.virtualFile

class RelationFunctionArgs : Structure<DTOPropArgs, List<Property>> {
    /**
     * @param element 关联属性中的方法参数元素
     *
     * @return 可用方法参数
     */
    override fun value(element: DTOPropArgs): List<Property> {
        val parentProp = element.parent.parent.parent.parent.parent as DTOPositiveProp
        return element.virtualFile
                .properties(element.project, parentProp.propPath())
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
package net.fallingangel.jimmerdto.completion.resolve.structure

import com.intellij.psi.util.parentOfType
import net.fallingangel.jimmerdto.enums.Modifier
import net.fallingangel.jimmerdto.enums.SpecFunction
import net.fallingangel.jimmerdto.psi.element.DTODto
import net.fallingangel.jimmerdto.psi.element.DTOPropName
import net.fallingangel.jimmerdto.structure.LookupInfo
import net.fallingangel.jimmerdto.util.modifiedBy

class PropFunctions : Structure<DTOPropName, List<LookupInfo>> {
    /**
     * @param element DTO或关联属性中的属性元素
     *
     * @return DTO中可用的方法列表
     */
    override fun value(element: DTOPropName): List<LookupInfo> {
        val dto = element.parentOfType<DTODto>() ?: return emptyList()
        val functions = listOf(
            LookupInfo("id", "id()", "function", "(<association>)", -1),
            LookupInfo("flat", "flat() {}", "function", "(<association>) { ... }", -4)
        )
        val specFunctions = if (dto modifiedBy Modifier.Specification) {
            SpecFunction.values()
                    .map {
                        with(it) {
                            val argPresentation = if (whetherMultiArg) {
                                "<prop, prop, prop, ...>"
                            } else {
                                "<prop>"
                            }
                            LookupInfo(
                                expression,
                                "$expression()",
                                "function",
                                "($argPresentation)",
                                -1
                            )
                        }
                    }
        } else {
            emptyList()
        }
        return functions + specFunctions
    }
}
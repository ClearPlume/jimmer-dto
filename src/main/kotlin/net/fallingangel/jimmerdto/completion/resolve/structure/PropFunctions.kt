package net.fallingangel.jimmerdto.completion.resolve.structure

import com.intellij.psi.util.parentOfType
import net.fallingangel.jimmerdto.enums.Modifier
import net.fallingangel.jimmerdto.enums.SpecFunction
import net.fallingangel.jimmerdto.psi.DTODto
import net.fallingangel.jimmerdto.psi.DTOPropName
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
            LookupInfo("id", "(<association>)", "function", "id()", -1),
            LookupInfo("flat", "(<association>) { ... }", "function", "flat() {}", -4)
        )
        val specFunctions = if (dto modifiedBy Modifier.SPECIFICATION) {
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
                                "($argPresentation)",
                                "function",
                                "$expression()",
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
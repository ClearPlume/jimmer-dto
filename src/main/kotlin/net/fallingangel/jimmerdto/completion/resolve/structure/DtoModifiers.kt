package net.fallingangel.jimmerdto.completion.resolve.structure

import net.fallingangel.jimmerdto.enums.Modifier
import net.fallingangel.jimmerdto.enums.modifiedBy
import net.fallingangel.jimmerdto.psi.DTODto
import net.fallingangel.jimmerdto.psi.DTODtoName

class DtoModifiers : Structure<DTODtoName, List<String>> {
    /**
     * @param element Dto名称元素
     *
     * @return Dto修饰符列表
     */
    override fun value(element: DTODtoName): List<String> {
        val dto = element.parent as DTODto
        val modifiers = mutableListOf("abstract", "input", "input-only", "inputOnly")
        if (dto modifiedBy Modifier.INPUT || dto modifiedBy Modifier.INPUT_ONLY) {
            modifiers -= "input"
            modifiers -= "input-only"
            modifiers -= "inputOnly"
        }
        if (dto modifiedBy Modifier.ABSTRACT) {
            modifiers -= "abstract"
        }
        return modifiers
    }
}
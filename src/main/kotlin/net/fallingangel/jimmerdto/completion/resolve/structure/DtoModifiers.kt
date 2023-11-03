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
        val allModifiers = Modifier.values().toMutableList()
        val dtoModifiers = dto.dtoModifierList.map { Modifier.valueOf(it.text.uppercase()) }

        if (dto modifiedBy Modifier.INPUT) {
            allModifiers -= Modifier.SPECIFICATION
        }
        if (dto modifiedBy Modifier.SPECIFICATION) {
            allModifiers -= Modifier.INPUT
        }
        return allModifiers
                .filter { it !in dtoModifiers }
                .map { it.value }
    }
}
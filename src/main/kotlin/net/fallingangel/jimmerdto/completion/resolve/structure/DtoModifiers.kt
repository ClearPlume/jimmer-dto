package net.fallingangel.jimmerdto.completion.resolve.structure

import net.fallingangel.jimmerdto.enums.Modifier
import net.fallingangel.jimmerdto.psi.DTODto
import net.fallingangel.jimmerdto.psi.DTODtoName
import net.fallingangel.jimmerdto.util.modifiedBy
import net.fallingangel.jimmerdto.util.toModifier

class DtoModifiers : Structure<DTODtoName, List<String>> {
    /**
     * @param element Dto名称元素
     *
     * @return Dto修饰符列表
     */
    override fun value(element: DTODtoName): List<String> {
        val dto = element.parent as DTODto
        val allModifiers = Modifier.values().toMutableList()
        val dtoModifiers = dto.modifierList.toModifier()

        if (dto modifiedBy Modifier.Input) {
            allModifiers -= Modifier.Specification
        }
        if (dto modifiedBy Modifier.Specification) {
            allModifiers -= Modifier.Input
        }
        return allModifiers
                .filter { it !in dtoModifiers }
                .map { it.name.lowercase() }
    }
}
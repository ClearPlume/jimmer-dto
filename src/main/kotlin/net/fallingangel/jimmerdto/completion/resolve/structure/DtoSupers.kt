package net.fallingangel.jimmerdto.completion.resolve.structure

import net.fallingangel.jimmerdto.enums.Modifier
import net.fallingangel.jimmerdto.enums.modifiedBy
import net.fallingangel.jimmerdto.enums.notModifiedBy
import net.fallingangel.jimmerdto.psi.DTODto
import net.fallingangel.jimmerdto.psi.DTODtoSupers
import net.fallingangel.jimmerdto.util.DTOPsiUtil

class DtoSupers : Structure<DTODtoSupers, List<String>> {
    /**
     * @param element Dto的父级Dto元素
     *
     * @return Dto的可用父级Dto列表
     */
    override fun value(element: DTODtoSupers): List<String> {
        val currentDto = element.parent as DTODto
        val supers = DTOPsiUtil.findDTOs(element).filter { it.dtoName.text != currentDto.dtoName.text }

        val availableSupers = if (currentDto modifiedBy Modifier.INPUT) {
            supers.filter { it modifiedBy Modifier.INPUT }
        } else if (currentDto modifiedBy Modifier.SPECIFICATION) {
            supers.filter { it modifiedBy Modifier.SPECIFICATION }
        } else if (currentDto notModifiedBy Modifier.UNSAFE) {
            supers.filter { it notModifiedBy Modifier.UNSAFE }
        } else {
            supers
        }
        return availableSupers.map { it.dtoName.text }
    }
}

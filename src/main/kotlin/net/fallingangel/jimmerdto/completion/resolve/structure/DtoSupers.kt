package net.fallingangel.jimmerdto.completion.resolve.structure

import net.fallingangel.jimmerdto.enums.Modifier
import net.fallingangel.jimmerdto.enums.modifiedBy
import net.fallingangel.jimmerdto.psi.DTODto
import net.fallingangel.jimmerdto.psi.DTODtoSupers
import net.fallingangel.jimmerdto.psi.impl.DTOPsiImplUtil

class DtoSupers : Structure<DTODtoSupers, List<String>> {
    /**
     * @param element Dto的父级Dto元素
     *
     * @return Dto的可用父级Dto列表
     */
    override fun value(element: DTODtoSupers): List<String> {
        val supers = DTOPsiImplUtil.findDTOs(element)
        val currentDto = element.parent as DTODto

        val availableSupers = if (currentDto.modifiedBy(Modifier.INPUT)) {
            supers.filter { it.modifiedBy(Modifier.INPUT) }
        } else if (currentDto.modifiedBy(Modifier.INPUT_ONLY)) {
            supers.filter { it.modifiedBy(Modifier.INPUT_ONLY) }
        } else {
            supers
        }
        return availableSupers.map { it.dtoName.name }
    }
}

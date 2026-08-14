package net.fallingangel.jimmerdto.psi.resolve

import net.fallingangel.jimmerdto.psi.element.DTOAlias

data class ImportEntry(val qualifiedName: String, val alias: DTOAlias?)

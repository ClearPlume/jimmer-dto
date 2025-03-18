package net.fallingangel.jimmerdto.psi.element

import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTOImplements : DTOElement {
    val implements: List<DTOTypeRef>
}
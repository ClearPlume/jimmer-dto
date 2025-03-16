package net.fallingangel.jimmerdto.psi.element

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.enums.Modifier
import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTODto : DTOElement {
    val annotations: List<DTOAnnotation>

    val modifierElements: List<PsiElement>

    val implements: DTOImplements?

    val name: DTODtoName

    val dtoBody: DTODtoBody

    val modifiers: List<Modifier>
        get() = modifierElements.map { modifier ->
            val value = modifier.text.replaceFirstChar { it.titlecase() }
            Modifier.valueOf(value)
        }
}
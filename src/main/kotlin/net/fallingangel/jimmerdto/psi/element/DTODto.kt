package net.fallingangel.jimmerdto.psi.element

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.enums.Modifier
import net.fallingangel.jimmerdto.lsi.LClass
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotationSite
import net.fallingangel.jimmerdto.lsi.annotation.hasAnnotation
import net.fallingangel.jimmerdto.lsi.jimmer.JimmerAnnotations
import net.fallingangel.jimmerdto.psi.DTOFile
import net.fallingangel.jimmerdto.psi.mixin.DTOAnnotationOwner
import net.fallingangel.jimmerdto.psi.mixin.DTOElement
import net.fallingangel.jimmerdto.util.modifiedBy

interface DTODto : DTOElement, DTOAnnotationOwner {
    val annotations: List<DTOAnnotation>

    val modifierElements: List<PsiElement>

    val implements: DTOImplements?

    val name: DTODtoName

    val dtoBody: DTODtoBody

    override val annotationSite: LAnnotationSite
        get() = LAnnotationSite.Type

    val clazz: LClass?
        get() = (containingFile as DTOFile).clazz

    val classIsEntity: Boolean
        get() = clazz?.hasAnnotation(JimmerAnnotations.Entity) == true

    val modifiers: List<Modifier>
        get() = modifierElements.map { modifier ->
            val value = modifier.text.replaceFirstChar { it.titlecase() }
            Modifier.valueOf(value)
        }

    val availableModifiers: List<String>
        get() {
            val allModifiers = Modifier.entries.toMutableList()

            if (this modifiedBy Modifier.Input) {
                allModifiers -= Modifier.Specification
            }
            if (this modifiedBy Modifier.Specification) {
                allModifiers -= Modifier.Input
            }

            return allModifiers
                .filter { it !in modifiers }
                .map { it.name.lowercase() }
        }
}

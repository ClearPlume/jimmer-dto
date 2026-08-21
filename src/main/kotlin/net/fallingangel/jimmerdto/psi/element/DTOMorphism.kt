package net.fallingangel.jimmerdto.psi.element

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.enums.Modifier
import net.fallingangel.jimmerdto.lsi.LClass
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotationSite
import net.fallingangel.jimmerdto.lsi.process
import net.fallingangel.jimmerdto.psi.mixin.DTOAnnotationOwner
import net.fallingangel.jimmerdto.psi.mixin.DTOElement
import net.fallingangel.jimmerdto.psi.resolve.Resolution

interface DTOMorphism : DTOElement, DTOAnnotationOwner {
    val annotations: List<DTOAnnotation>

    val modifierElement: PsiElement?

    val targetType: DTOQualifiedName?

    val classDeclaration: DTOClassDeclaration?

    val implements: DTOImplements?

    val dtoBody: DTODtoBody

    override val annotationSite: LAnnotationSite
        get() = LAnnotationSite.Type

    val modifier: Modifier?
        get() = modifierElement?.let { modifier ->
            val value = modifier.text.replaceFirstChar { it.titlecase() }
            Modifier.valueOf(value)
        }

    // morphism -> polymorphic
    val containingLClass: LClass?
        get() = (parent as DTOPolymorphic).containingLClass

    val resolvedLClass: LClass?
        get() = when (val target = targetType?.target) {
            is Resolution.Target.Subtype -> target.lClass
            is Resolution.Target.Type -> process(target.type) { lClass() }
            else -> null
        }
}

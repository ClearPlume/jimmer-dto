package net.fallingangel.jimmerdto.psi.element

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.enums.Modifier
import net.fallingangel.jimmerdto.lsi.LClass
import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTOMorphism : DTOElement {
    val annotations: List<DTOAnnotation>

    val modifierElement: PsiElement?

    val targetType: DTOQualifiedName?

    val classDeclaration: DTOClassDeclaration?

    val implements: DTOImplements?

    val dtoBody: DTODtoBody

    val modifier: Modifier?
        get() = modifierElement?.let { modifier ->
            val value = modifier.text.replaceFirstChar { it.titlecase() }
            Modifier.valueOf(value)
        }

    // morphism -> polymorphic
    val containingLClass: LClass?
        get() = (parent as DTOPolymorphic).containingLClass

    val resolvedLClass: LClass?
        get() {
            val targetType = this.targetType ?: return null
            val containingLClass = containingLClass ?: return null

            if (targetType.parts.size == 1) {
                val targetName = targetType.parts.first().part
                return containingLClass.children.find { it.name == targetName }
            } else {
                return targetType.resolvedLClass
            }
        }
}
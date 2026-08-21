package net.fallingangel.jimmerdto.psi.element

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.lsi.LClass
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotationSite
import net.fallingangel.jimmerdto.psi.mixin.DTOAnnotationOwner
import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTOUserProp : DTOElement, DTOAnnotationOwner {
    val annotations: List<DTOAnnotation>

    val name: DTOPropName

    val type: DTOTypeRef?

    val equals: PsiElement?

    val defaultValue: DTODefaultValue?

    override val annotationSite: LAnnotationSite
        get() = LAnnotationSite.Prop

    // userProp - dtoBody
    val containingLClass: LClass?
        get() = (parent as DTODtoBody).containingLClass
}

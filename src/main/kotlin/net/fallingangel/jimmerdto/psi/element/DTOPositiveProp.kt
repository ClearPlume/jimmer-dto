package net.fallingangel.jimmerdto.psi.element

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.enums.Modifier
import net.fallingangel.jimmerdto.psi.mixin.DTOElement

interface DTOPositiveProp : DTOElement {
    val annotations: List<DTOAnnotation>

    val configs: List<DTOPropConfig>

    val modifier: Modifier?

    val name: DTOPropName

    val flag: DTOPropFlag?

    val arg: DTOPropArg?

    val body: DTOPropBody?

    val alias: DTOAlias?

    val optional: PsiElement?

    val required: PsiElement?

    val recursive: PsiElement?
}
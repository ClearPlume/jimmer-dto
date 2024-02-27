package net.fallingangel.jimmerdto.psi.mixin

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.psi.DTOPropArgs
import net.fallingangel.jimmerdto.psi.DTOPropName

interface DTOSingleProp : PsiElement {
    val propName: DTOPropName

    val propArgs: DTOPropArgs?
        get() = null
}

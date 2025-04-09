package net.fallingangel.jimmerdto.psi.element

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.lsi.LClass
import net.fallingangel.jimmerdto.lsi.findProperty
import net.fallingangel.jimmerdto.psi.mixin.DTOElement
import net.fallingangel.jimmerdto.util.file
import net.fallingangel.jimmerdto.util.propPath

interface DTOMacro : DTOElement {
    val hash: PsiElement

    val name: DTOMacroName

    val args: DTOMacroArgs?

    val optional: PsiElement?

    val required: PsiElement?

    val clazz: LClass<*>
        get() {
            val propPath = propPath()
            return if (propPath.isEmpty()) {
                file.clazz
            } else {
                file.clazz.findProperty(propPath).actualType as LClass<*>
            }
        }

    val types: List<String>
        get() = clazz.allParents.map(LClass<*>::name) + clazz.name + "this"
}
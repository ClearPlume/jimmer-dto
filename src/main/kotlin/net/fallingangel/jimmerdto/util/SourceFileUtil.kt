package net.fallingangel.jimmerdto.util

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.PsiType
import com.intellij.psi.PsiWildcardType
import com.intellij.psi.util.PsiUtil

fun PsiType.clazz(): PsiClass? {
    val generic = PsiUtil.resolveGenericsClassInType(this)
    return if (generic.substitutor == PsiSubstitutor.EMPTY) {
        generic.element
    } else {
        val propTypeParameters = generic.element?.typeParameters ?: return null
        val genericType = generic.substitutor.substitute(propTypeParameters[0]) ?: return null

        if (genericType is PsiWildcardType) {
            if (genericType.isBounded) {
                genericType.clazz()
            } else {
                generic.element
            }
        } else {
            genericType.clazz()
        }
    }
}
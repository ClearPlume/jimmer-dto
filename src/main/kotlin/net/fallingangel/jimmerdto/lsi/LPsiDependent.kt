package net.fallingangel.jimmerdto.lsi

import com.intellij.psi.PsiElement

interface LPsiDependent {
    val source: PsiElement?

    fun collectPsiElements(result: MutableSet<PsiElement>, visited: MutableSet<LPsiDependent> = mutableSetOf())
}
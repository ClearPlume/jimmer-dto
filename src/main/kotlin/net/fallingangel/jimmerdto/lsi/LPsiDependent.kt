package net.fallingangel.jimmerdto.lsi

import com.intellij.psi.PsiElement

interface LPsiDependent {
    /**
     * 元素声明处 Psi，非使用处
     */
    val source: PsiElement?

    fun collectPsiElements(result: MutableSet<PsiElement>, visited: MutableSet<LPsiDependent> = mutableSetOf()) {
        if (!visited.add(this)) {
            return
        }
        source?.let(result::add)
        collectChildren(result, visited)
    }

    fun collectChildren(result: MutableSet<PsiElement>, visited: MutableSet<LPsiDependent>) {}
}
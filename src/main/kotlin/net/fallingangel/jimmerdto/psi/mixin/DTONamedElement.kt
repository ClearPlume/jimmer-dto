package net.fallingangel.jimmerdto.psi.mixin

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiReference

interface DTONamedElement : PsiNamedElement {
    override fun setName(name: String) = this

    // 获取元素中的引用
    operator fun invoke(): Array<PsiReference> = emptyArray()

    // 解析引用目标
    operator fun unaryPlus(): PsiElement? = null
}
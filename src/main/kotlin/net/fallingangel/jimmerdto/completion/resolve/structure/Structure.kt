package net.fallingangel.jimmerdto.completion.resolve.structure

import com.intellij.psi.PsiElement

/**
 * @param S 获取[S]类型的特定值
 * @param R 从[S]元素出发，要获取的值的类型
 */
interface Structure<S : PsiElement, R> {
    fun value(element: S): R
}

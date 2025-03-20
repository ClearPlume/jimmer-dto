package net.fallingangel.jimmerdto.completion.pattern

import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType

open class LsiPattern<T : PsiElement, Self : PsiElementPattern<T, Self>>(clazz: Class<T>) : PsiElementPattern<T, Self>(clazz) {
    override fun getParent(element: PsiElement): PsiElement? {
        return element.parent
    }

    class Capture<T : PsiElement>(clazz: Class<T>) : LsiPattern<T, Capture<T>>(clazz)
}

fun lsiElement() = lsiElement(PsiElement::class.java)

fun <T : PsiElement> lsiElement(clazz: Class<T>) = LsiPattern.Capture(clazz)

fun lsiElement(type: IElementType) = lsiElement().withElementType(type)
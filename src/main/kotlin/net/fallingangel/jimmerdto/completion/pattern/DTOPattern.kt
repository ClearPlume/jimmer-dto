package net.fallingangel.jimmerdto.completion.pattern

import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType

open class DTOPattern<T : PsiElement, Self : PsiElementPattern<T, Self>>(clazz: Class<T>) : PsiElementPattern<T, Self>(clazz) {
    override fun getParent(element: PsiElement): PsiElement? {
        return element.parent
    }

    class Capture<T : PsiElement>(clazz: Class<T>) : DTOPattern<T, Capture<T>>(clazz)
}

fun <T : PsiElement> dtoElement(clazz: Class<T>) = DTOPattern.Capture(clazz)

fun dtoElement() = dtoElement(PsiElement::class.java)

fun dtoElement(type: IElementType) = dtoElement().withElementType(type)

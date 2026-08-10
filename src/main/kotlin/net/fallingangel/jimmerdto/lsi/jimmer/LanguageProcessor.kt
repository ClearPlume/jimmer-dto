package net.fallingangel.jimmerdto.lsi.jimmer

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.lsi.LKind
import net.fallingangel.jimmerdto.lsi.LanguageProcessor

context(element: PsiElement)
fun LanguageProcessor.isEntity(): Boolean {
    return kind() == LKind.Interface && hasAnnotation(JimmerAnnotations.Entity)
}
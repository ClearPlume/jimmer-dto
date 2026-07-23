package net.fallingangel.jimmerdto.lsi.processor

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiNamedElement
import org.jetbrains.kotlin.idea.refactoring.memberInfo.KtPsiClassWrapper
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

fun PsiNamedElement.isInterfaceClass(): Boolean = when (this) {
    is KtClass -> isInterface()
    is PsiClass -> isInterface
    is KtPsiClassWrapper -> psiClass.isInterface
    else -> false
}

fun KtNamedDeclaration.isAbstract(): Boolean = when {
    hasModifier(KtTokens.ABSTRACT_KEYWORD) -> true
    containingClassOrObject?.isInterfaceClass() != true -> false
    this is KtProperty -> initializer == null && delegate == null && accessors.isEmpty()
    this is KtNamedFunction -> !hasBody()
    else -> false
}
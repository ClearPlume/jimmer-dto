package net.fallingangel.jimmerdto.lsi

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import net.fallingangel.jimmerdto.enums.StandardType
import org.jetbrains.kotlin.idea.base.util.module

private val EP = ExtensionPointName.create<CompilerContext>("net.fallingangel.compilerContext")

interface CompilerContext {
    fun appliesTo(module: Module): Boolean

    context(_: PsiElement)
    val precompiler: Precompiler

    context(element: PsiElement)
    fun builtinType(type: StandardType): PsiNamedElement?

    context(_: PsiElement)
    fun filterEntity(filterClass: PsiElement): PsiNamedElement?

    context(_: PsiElement)
    fun fieldFilterName(): LName
}

fun Module.context(): CompilerContext? {
    return EP.findFirstSafe { it.appliesTo(this) }
}

inline fun <R> compiling(element: PsiElement, action: context(PsiElement) CompilerContext.() -> R): R? {
    return element.module?.context()?.let { action(element, it) }
}

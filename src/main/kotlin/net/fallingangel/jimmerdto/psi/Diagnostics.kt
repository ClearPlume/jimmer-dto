package net.fallingangel.jimmerdto.psi

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.psi.mixin.DTOElement
import kotlin.reflect.KCallable

fun DTOElement.grammarMismatch(): Nothing {
    error("No branch matched for ${node.elementType} at ${containingFile.name}:$textOffset, text: $text")
}

fun PsiElement.unhandledElement(): Nothing {
    error("Unhandled element type ${node.elementType} at ${containingFile.name}:$textOffset, text: $text")
}

fun <R : Any> PsiElement.demand(declaration: KCallable<R?>, vararg args: Any?): R {
    return declaration.call(this, *args) ?: missing(declaration.name)
}

fun PsiElement.missing(what: String): Nothing {
    error("Missing $what on ${node.elementType} at ${containingFile.name}:$textOffset, text: $text")
}

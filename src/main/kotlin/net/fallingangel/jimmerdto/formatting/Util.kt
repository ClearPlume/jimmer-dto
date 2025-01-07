package net.fallingangel.jimmerdto.formatting

import com.intellij.formatting.SpacingBuilder
import com.intellij.psi.tree.IElementType

fun SpacingBuilder.RuleBuilder.emptyLine(lines: Int = 1): SpacingBuilder {
    return spacing(0, 0, lines + 1, false, 0)
}

fun SpacingBuilder.around(elementType: IElementType, left: Int = 0, right: Int = 1): SpacingBuilder {
    return before(elementType).spaces(left).after(elementType).spaces(right)
}
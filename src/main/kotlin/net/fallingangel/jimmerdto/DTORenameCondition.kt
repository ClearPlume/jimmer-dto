package net.fallingangel.jimmerdto

import com.intellij.openapi.util.Condition
import net.fallingangel.jimmerdto.psi.element.DTOMacroName
import net.fallingangel.jimmerdto.psi.mixin.DTOElement

/**
 * 元素重命名控制条件，返回true则禁止重命名
 */
class DTORenameCondition : Condition<DTOElement> {
    override fun value(element: DTOElement?): Boolean {
        return element is DTOMacroName
    }
}
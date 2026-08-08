package net.fallingangel.jimmerdto.refactor

import com.intellij.openapi.util.Condition
import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.psi.element.DTOMacroName
import net.fallingangel.jimmerdto.psi.element.DTOPositiveProp

/**
 * 元素重命名控制条件，返回true则禁止重命名
 */
class DTORenameCondition : Condition<PsiElement> {
    override fun value(element: PsiElement?): Boolean {
        return element is DTOMacroName || (element?.parent as? DTOPositiveProp)?.arg != null
    }
}
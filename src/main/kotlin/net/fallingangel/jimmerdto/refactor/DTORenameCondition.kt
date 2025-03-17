package net.fallingangel.jimmerdto.refactor

import com.intellij.openapi.util.Condition
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.util.CommonRefactoringUtil
import net.fallingangel.jimmerdto.psi.element.DTOMacroName
import net.fallingangel.jimmerdto.psi.mixin.DTOElement
import org.jetbrains.kotlin.idea.codeinsight.utils.findExistingEditor

/**
 * 元素重命名控制条件，返回true则禁止重命名
 */
class DTORenameCondition : Condition<DTOElement> {
    override fun value(element: DTOElement?): Boolean {
        if (element is DTOMacroName) {
            CommonRefactoringUtil.showErrorHint(
                element.project,
                element.findExistingEditor(),
                RefactoringBundle.message("error.cannot.be.renamed"),
                RefactoringBundle.message("rename.title"),
                null,
            )
        }
        return element is DTOMacroName
    }
}
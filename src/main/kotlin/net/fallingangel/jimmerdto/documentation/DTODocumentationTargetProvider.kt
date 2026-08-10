package net.fallingangel.jimmerdto.documentation

import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.DocumentationTargetProvider
import com.intellij.psi.PsiFile
import net.fallingangel.jimmerdto.psi.DTOFile
import net.fallingangel.jimmerdto.psi.element.DTOMacro
import net.fallingangel.jimmerdto.psi.element.DTOMacroName

class DTODocumentationTargetProvider : DocumentationTargetProvider {
    override fun documentationTargets(file: PsiFile, offset: Int): List<DocumentationTarget> {
        if (file !is DTOFile) return emptyList()
        val element = file.findElementAt(offset)?.parent ?: return emptyList()

        // 宏文档
        if (element is DTOMacroName) {
            val macro = element.parent as DTOMacro
            return listOf(DTOMacroDocumentationTarget(macro))
        }

        return emptyList()
    }
}

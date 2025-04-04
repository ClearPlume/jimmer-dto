package net.fallingangel.jimmerdto.psi.fix

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import net.fallingangel.jimmerdto.psi.element.DTODto
import net.fallingangel.jimmerdto.psi.element.createModifier

class ReorderingModifier(private val dto: DTODto) : BaseFix() {
    override fun getText() = "Reordering modifiers"

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        val orderedModifiers = dto.modifierElements.zip(dto.modifiers)
                .sortedBy { it.second.order }
                .map { project.createModifier(it.first.text) }
        WriteCommandAction.runWriteCommandAction(project) {
            dto.modifierElements.zip(orderedModifiers)
                    .forEach { (modifier, orderedModifier) -> modifier.replace(orderedModifier) }
        }
    }
}
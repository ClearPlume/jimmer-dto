package net.fallingangel.jimmerdto.psi.fix

import com.intellij.modcommand.ActionContext
import com.intellij.modcommand.ModPsiUpdater
import com.intellij.modcommand.PsiUpdateModCommandAction
import net.fallingangel.jimmerdto.psi.element.DTODto
import net.fallingangel.jimmerdto.psi.element.createModifier

@Suppress("UnstableApiUsage")
class ReorderingModifier(dto: DTODto) : PsiUpdateModCommandAction<DTODto>(dto) {
    override fun getFamilyName() = "Reordering modifiers"

    override fun invoke(context: ActionContext, element: DTODto, updater: ModPsiUpdater) {
        val project = context.project
        val orderedModifiers = element.modifierElements.zip(element.modifiers)
                .sortedBy { it.second.order }
                .map { project.createModifier(it.first.text) }
        element.modifierElements.zip(orderedModifiers)
                .forEach { (modifier, orderedModifier) -> modifier.replace(orderedModifier) }
    }
}
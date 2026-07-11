package net.fallingangel.jimmerdto.psi.fix

import com.intellij.modcommand.ActionContext
import com.intellij.modcommand.ModPsiUpdater
import com.intellij.modcommand.PsiUpdateModCommandAction
import net.fallingangel.jimmerdto.psi.element.DTOAliasGroup
import net.fallingangel.jimmerdto.psi.element.createAliasGroup

@Suppress("UnstableApiUsage")
class InsertArrow(element: DTOAliasGroup) : PsiUpdateModCommandAction<DTOAliasGroup>(element) {
    override fun getFamilyName() = "Add `->` to alias group"

    override fun invoke(context: ActionContext, element: DTOAliasGroup, updater: ModPsiUpdater) {
        println(element.power?.text)
        println(element.original?.text)
        println(element.dollar?.text)
        println(element.replacement?.text)
        element.replace(
            context.project.createAliasGroup(
                element.power?.text ?: "",
                element.original?.text ?: "",
                element.dollar?.text ?: "",
                "->",
                element.replacement?.text ?: "",
            )
        )
    }
}
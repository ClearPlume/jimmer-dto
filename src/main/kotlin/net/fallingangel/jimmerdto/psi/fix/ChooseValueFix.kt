package net.fallingangel.jimmerdto.psi.fix

import com.intellij.codeInsight.template.impl.ConstantNode
import com.intellij.modcommand.ActionContext
import com.intellij.modcommand.ModPsiUpdater
import com.intellij.modcommand.PsiUpdateModCommandAction
import com.intellij.psi.PsiElement

@Suppress("UnstableApiUsage")
class ChooseValueFix(element: PsiElement, private val candidates: List<String>) : PsiUpdateModCommandAction<PsiElement>(element) {
    override fun getFamilyName() = "Choose value"

    override fun invoke(context: ActionContext, element: PsiElement, updater: ModPsiUpdater) {
        updater.templateBuilder().field(element, ConstantNode(element.text).withLookupStrings(candidates))
    }
}

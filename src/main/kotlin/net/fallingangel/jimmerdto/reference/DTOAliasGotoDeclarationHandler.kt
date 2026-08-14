package net.fallingangel.jimmerdto.reference

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandlerBase
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.core.DTOLanguage
import net.fallingangel.jimmerdto.psi.element.DTOAlias
import net.fallingangel.jimmerdto.psi.resolve.Resolution

class DTOAliasGotoDeclarationHandler : GotoDeclarationHandlerBase() {
    override fun getGotoDeclarationTarget(sourceElement: PsiElement?, editor: Editor): PsiElement? {
        if (sourceElement?.language != DTOLanguage) return null

        val alias = sourceElement.parent as? DTOAlias ?: return null
        val target = alias.target as? Resolution.Target.Type ?: return null
        return target.source
    }
}

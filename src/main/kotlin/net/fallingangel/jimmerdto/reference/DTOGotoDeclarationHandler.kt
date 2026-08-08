package net.fallingangel.jimmerdto.reference

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.core.DTOLanguage
import net.fallingangel.jimmerdto.psi.element.DTOQualifiedNamePart
import net.fallingangel.jimmerdto.psi.resolve.Resolution.Target.Property as TargetProperty

class DTOGotoDeclarationHandler : GotoDeclarationHandler {
    override fun getGotoDeclarationTargets(sourceElement: PsiElement?, offset: Int, editor: Editor): Array<PsiElement?>? {
        if (sourceElement?.language != DTOLanguage) return null

        val parent = sourceElement.parent as? DTOQualifiedNamePart ?: return null
        val target = parent.target as? TargetProperty ?: return null
        val via = target.via as? TargetProperty.Via.ImplicitId ?: return null

        return arrayOf(via.reference.source, target.source)
    }
}

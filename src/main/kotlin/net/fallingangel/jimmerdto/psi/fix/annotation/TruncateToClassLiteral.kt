package net.fallingangel.jimmerdto.psi.fix.annotation

import com.intellij.modcommand.ActionContext
import com.intellij.modcommand.ModPsiUpdater
import com.intellij.modcommand.PsiUpdateModCommandAction
import net.fallingangel.jimmerdto.psi.element.DTOAnnotationSingleValue
import net.fallingangel.jimmerdto.psi.resolve.Resolution

@Suppress("UnstableApiUsage")
class TruncateToClassLiteral(
    annotationValue: DTOAnnotationSingleValue,
) : PsiUpdateModCommandAction<DTOAnnotationSingleValue>(annotationValue) {
    override fun getFamilyName() = "Truncate to class literal"

    override fun invoke(context: ActionContext, element: DTOAnnotationSingleValue, updater: ModPsiUpdater) {
        val qualifiedName = element.qualifiedName ?: return
        val parts = qualifiedName.parts
        val last = parts.indexOfLast { it.target is Resolution.Target.Type }
        qualifiedName.deleteChildRange(parts[last].nextSibling, parts.last())
    }
}

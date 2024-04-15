package net.fallingangel.jimmerdto

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import icons.Icons
import net.fallingangel.jimmerdto.psi.DTODto
import net.fallingangel.jimmerdto.psi.DTOTypes
import net.fallingangel.jimmerdto.util.classFile
import net.fallingangel.jimmerdto.util.nameIdentifier
import net.fallingangel.jimmerdto.util.open
import org.jetbrains.kotlin.idea.core.util.toPsiFile

class DTOLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(element: PsiElement, result: MutableCollection<in RelatedItemLineMarkerInfo<*>>) {
        // 针对DTO名称元素发起跳转
        if (element.elementType == DTOTypes.IDENTIFIER && element.parent.elementType == DTOTypes.DTO_NAME && element.parent.parent.elementType == DTOTypes.DTO) {
            val project = element.project
            val dto = element.parent.parent as? DTODto ?: return
            val dtoName = dto.dtoName.text

            val dtoClassFile = dto.classFile() ?: return
            val nameIdentifier = dtoClassFile.nameIdentifier(project) ?: return
            result.add(
                NavigationGutterIconBuilder.create(Icons.icon_16)
                        .setTargets(dtoClassFile.toPsiFile(project))
                        .setTooltipText("Jump to generated class [$dtoName]")
                        .createLineMarkerInfo(element) { _, _ -> dtoClassFile.open(project, nameIdentifier.textOffset) }
            )
        }
    }
}

package net.fallingangel.jimmerdto

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.util.elementType
import icons.Icons
import net.fallingangel.jimmerdto.psi.DTODtoName
import net.fallingangel.jimmerdto.psi.DTOExportStatement
import net.fallingangel.jimmerdto.psi.DTOFile
import net.fallingangel.jimmerdto.psi.DTOTypes
import net.fallingangel.jimmerdto.util.fqe
import net.fallingangel.jimmerdto.util.qualified
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType

class DTOLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(element: PsiElement, result: MutableCollection<in RelatedItemLineMarkerInfo<*>>) {
        // 针对DTO名称元素发起跳转
        if (element.elementType == DTOTypes.IDENTIFIER && element.parent.elementType == DTOTypes.DTO_NAME) {
            val project = element.project
            val dtoName = element.parent as? DTODtoName ?: return
            val dtoFile = dtoName.containingFile as DTOFile

            val `package` = dtoFile.getChildOfType<DTOExportStatement>()?.packageStatement?.qualified ?: dtoName.fqe.substringBeforeLast('.')
            val dtoClass = JavaPsiFacade.getInstance(project).findClass("$`package`.dto.${dtoName.text}", ProjectScope.getAllScope(project)) ?: return

            result.add(
                NavigationGutterIconBuilder.create(Icons.icon_16)
                        .setTargets(dtoClass)
                        .setTooltipText("Jump to generated class [${dtoName.text}]")
                        .createLineMarkerInfo(element)
            )
        }
    }
}

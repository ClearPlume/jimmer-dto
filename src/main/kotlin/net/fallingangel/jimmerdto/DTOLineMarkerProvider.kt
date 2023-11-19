package net.fallingangel.jimmerdto

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import icons.Icons
import net.fallingangel.jimmerdto.enums.Modifier
import net.fallingangel.jimmerdto.enums.notModifiedBy
import net.fallingangel.jimmerdto.psi.DTODto
import net.fallingangel.jimmerdto.psi.DTOExport
import net.fallingangel.jimmerdto.psi.DTOTypes
import net.fallingangel.jimmerdto.util.*
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import java.nio.file.Paths

class DTOLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(element: PsiElement, result: MutableCollection<in RelatedItemLineMarkerInfo<*>>) {
        // 针对DTO名称元素发起跳转
        if (element.elementType == DTOTypes.IDENTIFIER && element.parent.elementType == DTOTypes.DTO_NAME && element.parent.parent.elementType == DTOTypes.DTO) {
            val project = element.project
            val dtoFile = element.virtualFile
            val export = dtoFile.psiFile(project)?.getChildOfType<DTOExport>()
            val `package` = export?.`package`

            // 获取GenerateSource源码路径
            val generateRoot = generateRoot(element) ?: return
            val fileManager = VirtualFileManager.getInstance()

            val dtoPath = if (export != null && `package` != null) {
                /* 获取package关键字定义的dto类路径 */
                // package指定的包名转化为路径
                `package`.qualifiedType.text.replace('.', '/')
            } else {
                /* 获取默认的dto文件生成类路径 */
                // 获取dto根路径
                val dtoRoot = dtoRoot(element)?.path ?: return
                // 获取dto相对dto根路径的路径
                dtoFile.path.removePrefix(dtoRoot).replace(Regex("/(.+)/.+?dto$"), "$1/dto")
            }
            val generateDtoRoot = fileManager.findFileByNioPath(Paths.get("${generateRoot.path}/$dtoPath")) ?: return

            val dto = element.parent.parent as? DTODto ?: return
            val dtoName = dto.dtoName.text

            if (dto notModifiedBy Modifier.ABSTRACT) {
                val lineMarker = generateDtoRoot.createLineMarker(project, element, dtoName) ?: return
                result.add(lineMarker)
            }
        }
    }

    private fun VirtualFile.createLineMarker(project: Project, element: PsiElement, dtoName: String): RelatedItemLineMarkerInfo<PsiElement>? {
        val dtoFile = children.find { it.name.split('.')[0] == dtoName } ?: return null
        val nameIdentifier = dtoFile.nameIdentifier(project) ?: return null

        return NavigationGutterIconBuilder.create(Icons.icon_16)
                .setTargets(dtoFile.psiFile(project))
                .setTooltipText("Jump to generated class [$dtoName]")
                .createLineMarkerInfo(element) { _, _ -> dtoFile.open(project, nameIdentifier.textOffset) }
    }
}

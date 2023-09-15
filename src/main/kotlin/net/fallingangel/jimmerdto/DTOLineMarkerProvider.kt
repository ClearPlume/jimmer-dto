package net.fallingangel.jimmerdto

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import net.fallingangel.jimmerdto.psi.DTODto
import net.fallingangel.jimmerdto.psi.DTOTypes
import org.jetbrains.kotlin.psi.KtClass
import java.nio.file.Paths

class DTOLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(element: PsiElement, result: MutableCollection<in RelatedItemLineMarkerInfo<*>>) {
        if (element.elementType == DTOTypes.IDENTIFIER && element.parent.elementType == DTOTypes.DTO_NAME && element.parent.parent.elementType == DTOTypes.DTO) {
            val project = element.project
            // 获取生成的源码路径
            val generateRoot = generateRoot(element) ?: return
            // 获取dto文件生成的类路径
            val generateDtoRoot = VirtualFileManager.getInstance().findFileByNioPath(Paths.get("${generateRoot.path}/model/dto")) ?: return
            // 获取项目类型，两种可能值：java，kt
            val javaORkt = generateDtoRoot.children.firstOrNull()?.name?.split('.')?.get(1) ?: return

            val dto = element.parent.parent as? DTODto ?: return
            val dtoName = dto.dtoName.text
            val dtoModifiers = dto.dtoModifierList

            if (dtoModifiers.none { it.text == "abstract" }) {
                result.add(
                    if (javaORkt == "java") {
                        generateDtoRoot.createLineMarker<PsiClass>(project, element, dtoName)
                    } else {
                        generateDtoRoot.createLineMarker<KtClass>(project, element, dtoName)
                    }
                )
            }
        }
    }

    private inline fun <reified T : PsiNameIdentifierOwner> VirtualFile.createLineMarker(project: Project, element: PsiElement, dtoName: String): RelatedItemLineMarkerInfo<PsiElement> {
        val psiManager = PsiManager.getInstance(project)
        val dtoFile = psiManager.findFile(children.find { it.name.split('.')[0] == dtoName }!!)!!
        val dtoClass = PsiTreeUtil.findChildOfType(dtoFile.originalElement, T::class.java)!!

        return NavigationGutterIconBuilder.create(Constant.ICON)
                .setTargets(dtoFile)
                .setTooltipText("Jump to generated class [$dtoName]")
                .createLineMarkerInfo(element) { _, _ ->
                    val openFileDescriptor = OpenFileDescriptor(project, dtoFile.virtualFile, dtoClass.nameIdentifier!!.textOffset)
                    FileEditorManager.getInstance(project).openEditor(openFileDescriptor, true)
                }
    }
}

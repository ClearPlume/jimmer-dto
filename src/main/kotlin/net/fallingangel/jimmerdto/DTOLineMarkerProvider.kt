package net.fallingangel.jimmerdto

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.util.indexing.FileBasedIndex
import icons.Icons
import net.fallingangel.jimmerdto.index.DTO_ENTITY_INDEX
import net.fallingangel.jimmerdto.lsi.LKind
import net.fallingangel.jimmerdto.lsi.jimmer.JimmerAnnotations
import net.fallingangel.jimmerdto.lsi.process
import net.fallingangel.jimmerdto.psi.element.DTODtoName
import org.jetbrains.kotlin.idea.base.util.projectScope
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.KtClass

class DTOLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(element: PsiElement, result: MutableCollection<in RelatedItemLineMarkerInfo<*>>) {
        // DTO 名称
        if (element is DTODtoName) {
            val dtoClass = element.resolve() ?: return
            val anchor = element.nameIdentifier ?: return
            result.add(
                NavigationGutterIconBuilder.create(Icons.PluginIcon)
                    .setTargets(dtoClass)
                    .setTooltipText("Jump to generated class '${element.value}'")
                    .createLineMarkerInfo(anchor)
            )
            return
        }

        // 实体
        if (element is PsiClass || element is KtClass) {
            val entityName = process(element) {
                if (kind() == LKind.Interface && hasAnnotation(JimmerAnnotations.Entity)) {
                    classQualifiedName()
                } else {
                    null
                }
            }

            if (entityName != null) {
                val files = FileBasedIndex.getInstance().getContainingFiles(DTO_ENTITY_INDEX, entityName, element.project.projectScope())
                if (files.isNotEmpty()) {
                    val anchor = (element as? PsiNameIdentifierOwner)?.nameIdentifier ?: return
                    result.add(
                        NavigationGutterIconBuilder.create(Icons.PluginIcon)
                            .setTargets(files.mapNotNull { it.toPsiFile(element.project) })
                            .setTooltipText("Jump to DTO file [${files.joinToString { it.name }}]")
                            .createLineMarkerInfo(anchor)
                    )
                }
            }
        }
    }
}

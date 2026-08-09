package net.fallingangel.jimmerdto.project.sourceroot

import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.roots.ui.configuration.ModuleSourceRootEditHandler
import com.intellij.ui.JBColor
import icons.Icons
import org.jetbrains.jps.model.JpsDummyElement
import org.jetbrains.jps.model.module.JpsModuleSourceRootType
import javax.swing.Icon

sealed class DtoSourceRootEditHandler(
    rootType: JpsModuleSourceRootType<JpsDummyElement>,
) : ModuleSourceRootEditHandler<JpsDummyElement>(rootType) {

    override fun getFolderUnderRootIcon(): Icon? = null

    override fun getMarkRootShortcutSet(): CustomShortcutSet = CustomShortcutSet.EMPTY
}

class DtoProductionSourceRootEditHandler : DtoSourceRootEditHandler(DtoSourceRootType.SOURCE) {
    override fun getRootTypeName() = "DTO Source"
    override fun getRootIcon() = Icons.SourceRoot
    override fun getRootsGroupTitle() = "DTO Sources"
    override fun getRootsGroupColor() = JBColor(0x6B50B7, 0xA19EF7)
    override fun getUnmarkRootButtonText() = "Unmark DTO Source"
}

class DtoTestSourceRootEditHandler : DtoSourceRootEditHandler(DtoSourceRootType.TEST_SOURCE) {
    override fun getRootTypeName() = "DTO Test Source"
    override fun getRootIcon() = Icons.TestRoot
    override fun getRootsGroupTitle() = "DTO Test Sources"
    override fun getRootsGroupColor() = JBColor(0xEB6482, 0xFB9CB6)
    override fun getUnmarkRootButtonText() = "Unmark DTO Test Source"
}
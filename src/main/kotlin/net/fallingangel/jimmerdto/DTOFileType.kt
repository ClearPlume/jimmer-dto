package net.fallingangel.jimmerdto

import com.intellij.openapi.fileTypes.LanguageFileType
import icons.Icons

class DTOFileType : LanguageFileType(DTOLanguage.INSTANCE) {
    override fun getName() = Constant.NAME

    override fun getDescription() = "Jimmer框架的DTO转换语言"

    override fun getDefaultExtension() = Constant.EXTENSION

    override fun getIcon() = Icons.icon_16

    @Suppress("CompanionObjectInExtension")
    companion object {
        @JvmField
        val INSTANCE = DTOFileType()
    }
}

package net.fallingangel.jimmerdto

import com.intellij.openapi.fileTypes.LanguageFileType
import icons.Icons

class DTOFileType : LanguageFileType(DTOLanguage) {
    override fun getName() = LANGUAGE_NAME

    override fun getDescription() = "Jimmer框架的DTO转换语言"

    override fun getDefaultExtension() = "dto"

    override fun getIcon() = Icons.icon_16

    @Suppress("CompanionObjectInExtension")
    companion object {
        val INSTANCE = DTOFileType()
    }
}

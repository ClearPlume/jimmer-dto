package net.fallingangel.jimmerdto

import com.intellij.openapi.fileTypes.LanguageFileType

class DTOFileType : LanguageFileType(DTOLanguage.INSTANCE) {
    override fun getName() = Constant.NAME

    override fun getDescription() = "Jimmer框架的DTO转换语言"

    override fun getDefaultExtension() = Constant.EXTENSION

    override fun getIcon() = Constant.ICON

    @Suppress("CompanionObjectInExtension")
    companion object {
        @JvmField
        val INSTANCE = DTOFileType()
    }
}

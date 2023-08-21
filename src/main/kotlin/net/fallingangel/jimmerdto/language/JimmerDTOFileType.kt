package net.fallingangel.jimmerdto.language

import com.intellij.openapi.fileTypes.LanguageFileType
import net.fallingangel.jimmerdto.Constant

class JimmerDTOFileType : LanguageFileType(JimmerDTOLanguage.INSTANCE) {
    override fun getName() = Constant.NAME

    override fun getDescription() = "Jimmer框架的DTO转换语言"

    override fun getDefaultExtension() = Constant.EXTENSION

    override fun getIcon() = Constant.ICON

    @Suppress("CompanionObjectInExtension")
    companion object {
        @Suppress("unused")
        val INSTANCE = JimmerDTOFileType()
    }
}

package net.fallingangel.jimmerdto

import com.intellij.openapi.util.IconLoader
import net.fallingangel.jimmerdto.language.DTOLanguage

object Constant {
    const val NAME = "JimmerDTO"

    const val EXTENSION = "dto"

    val ICON = IconLoader.getIcon("/languageIcon.png", DTOLanguage::class.java)
}

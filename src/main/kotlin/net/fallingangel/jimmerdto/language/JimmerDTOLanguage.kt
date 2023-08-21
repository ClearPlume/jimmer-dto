package net.fallingangel.jimmerdto.language

import com.intellij.lang.Language
import net.fallingangel.jimmerdto.Constant

class JimmerDTOLanguage : Language(Constant.NAME) {
    companion object {
        val INSTANCE = JimmerDTOLanguage()
    }
}

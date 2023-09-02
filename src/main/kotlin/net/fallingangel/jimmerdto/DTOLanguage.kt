package net.fallingangel.jimmerdto

import com.intellij.lang.Language

class DTOLanguage : Language(Constant.NAME) {
    companion object {
        val INSTANCE = DTOLanguage()
    }
}

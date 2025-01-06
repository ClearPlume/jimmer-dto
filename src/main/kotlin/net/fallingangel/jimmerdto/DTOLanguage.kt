package net.fallingangel.jimmerdto

import com.intellij.lang.Language

object DTOLanguage : Language(Constant.NAME) {
    private fun readResolve(): Any = DTOLanguage
}
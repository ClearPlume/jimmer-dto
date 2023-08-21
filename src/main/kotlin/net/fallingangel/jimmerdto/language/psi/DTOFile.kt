package net.fallingangel.jimmerdto.language.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.FileViewProvider
import net.fallingangel.jimmerdto.language.DTOFileType
import net.fallingangel.jimmerdto.language.DTOLanguage

class DTOFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, DTOLanguage.INSTANCE) {
    override fun getFileType() = DTOFileType.INSTANCE

    override fun toString() = "JimmerDTO File"
}

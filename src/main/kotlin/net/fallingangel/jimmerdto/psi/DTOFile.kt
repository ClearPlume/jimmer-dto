package net.fallingangel.jimmerdto.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.FileViewProvider
import net.fallingangel.jimmerdto.DTOFileType
import net.fallingangel.jimmerdto.DTOLanguage

class DTOFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, DTOLanguage.INSTANCE) {
    override fun getFileType() = DTOFileType.INSTANCE

    override fun toString() = "JimmerDTO File"
}

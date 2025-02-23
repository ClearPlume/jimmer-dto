package net.fallingangel.jimmerdto.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.FileViewProvider
import net.fallingangel.jimmerdto.DTOFileType
import net.fallingangel.jimmerdto.DTOLanguage
import net.fallingangel.jimmerdto.util.qualified
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType

class DTOFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, DTOLanguage) {
    val implicitPackage: String
        get() {
            val fileIndex = ProjectRootManager.getInstance(project).fileIndex
            val root = fileIndex.getContentRootForFile(virtualFile)?.path ?: throw IllegalStateException("Source root is null")
            return virtualFile.path // dto文件
                    // dto文件相对根路径的子路径
                    .removePrefix("$root/")
                    // 移除『dto/』前缀
                    .substringAfter('/')
                    // 移除『name.dto』后缀
                    .substringBefore("/$name")
                    .replace('/', '.')
        }

    val `package`: String
        get() {
            val export = getChildOfType<DTOExportStatement>()
            val `package` = export?.getChildOfType<DTOPackageStatement>()
            return `package`?.qualified
                ?: export?.qualified?.substringBeforeLast('.')?.let { "$it.dto" }
                ?: "$implicitPackage.dto"
        }

    override fun getFileType() = DTOFileType.INSTANCE

    override fun toString() = "JimmerDTO File"
}

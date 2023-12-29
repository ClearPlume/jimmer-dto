package net.fallingangel.jimmerdto.util

import com.intellij.codeInsight.completion.CompletionUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.psi.DTOAliasGroup
import net.fallingangel.jimmerdto.psi.DTODto
import net.fallingangel.jimmerdto.psi.DTOExport
import net.fallingangel.jimmerdto.psi.DTOPositiveProp
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import java.nio.file.Paths

val DTOPositiveProp.name: String?
    get() = propName.text.takeIf { it != CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED }

/**
 * 元素是否包含上一层级的属性级结构
 *
 * 元素：宏、属性、负属性、方法等
 * 属性级结构：flat方法、as组等
 */
val <T : PsiElement> T.haveUpper: Boolean
    get() = parent.parent is DTOAliasGroup || parent.parent.parent.parent is DTOPositiveProp

val <T : PsiElement> T.upper: PsiElement
    get() {
        return if (parent.parent is DTOAliasGroup) {
            parent.parent
        } else {
            parent.parent.parent.parent
        }
    }

fun <T : PsiElement> T.propPath(): List<String> {
    val propName = if (this is DTOPositiveProp) {
        if (name == "flat") {
            listOf(propArgs!!.valueList[0].text)
        } else {
            name?.let { listOf(it) } ?: emptyList()
        }
    } else if (this is DTOAliasGroup) {
        emptyList()
    } else {
        throw UnsupportedOperationException("Only support find path for prop, alias-group")
    }

    if (parent.parent.parent is DTODto) {
        return propName
    }
    return upper.propPath() + propName
}

val DTODto.classFile: VirtualFile?
    get() {
        val dtoName = dtoName.text
        // 获取GenerateSource源码路径
        val generateRoot = generateRoot(this) ?: return null
        val fileManager = VirtualFileManager.getInstance()
        val export = virtualFile.psiFile(project)?.getChildOfType<DTOExport>()
        val `package` = export?.`package`

        val dtoPath = if (export != null) {
            /* 获取package关键字定义的dto类路径 */
            val packageDtoPath = `package`?.qualifiedType?.text?.replace('.', '/')
            /* 若没有指定package，则获取export关键字指定的类路径对应的dto类路径 */
            packageDtoPath ?: (export.qualifiedType.text.substringBeforeLast('.') + ".dto").replace('.', '/')
        } else {
            /* 获取默认的dto文件生成类路径 */
            // 获取dto根路径
            val dtoRoot = dtoRoot(this)?.path ?: return null
            // 获取dto相对dto根路径的路径
            virtualFile.path.removePrefix(dtoRoot).replace(Regex("/(.+)/.+?dto$"), "$1/dto")
        }
        val generateDtoRoot = fileManager.findFileByNioPath(Paths.get("${generateRoot.path}/$dtoPath")) ?: return null
        return generateDtoRoot.children.find { it.name.split('.')[0] == dtoName }
    }

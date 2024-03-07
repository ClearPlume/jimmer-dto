package net.fallingangel.jimmerdto.util

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import net.fallingangel.jimmerdto.completion.resolve.structure.Structure
import net.fallingangel.jimmerdto.psi.*
import net.fallingangel.jimmerdto.psi.mixin.DTOSingleProp
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import java.nio.file.Paths

/**
 * 元素是否包含上一层级的属性级结构
 *
 * 元素：宏、属性、负属性、方法等
 * 属性级结构：flat方法、as组等
 */
val PsiElement.haveUpper: Boolean
    get() {
        if (this is DTOFile) {
            return false
        }
        return parent.parent is DTOAliasGroup || parent.parent.parent.parent is DTOPositiveProp
    }

val PsiElement.upper: PsiElement
    get() {
        return if (parent.parent is DTOAliasGroup) {
            parent.parent
        } else {
            parent.parent.parent.parent
        }
    }

val PsiElement.virtualFile: VirtualFile
    get() = containingFile.originalFile.virtualFile

inline fun <reified T : PsiElement> PsiElement.haveParent() = parentOfType<T>() != null

fun <T : DTOSingleProp> T.psiClass(): PsiClass {
    val entityFile = virtualFile.entityFile(project) ?: throw IllegalStateException()
    return entityFile.psiClass(project, upper.propPath()) ?: throw IllegalStateException()
}

fun <T : DTOSingleProp> T.ktClass(): KtClass {
    val entityFile = virtualFile.entityFile(project) ?: throw IllegalStateException()
    return entityFile.ktClass(project, upper.propPath()) ?: throw IllegalStateException()
}

operator fun <S : PsiElement, R, T : Structure<S, R>> S.get(type: T): R {
    return type.value(this)
}

fun PsiElement.propPath(): List<String> {
    val propName = if (this is DTOPositiveProp) {
        if (propName.text == "flat") {
            listOf(propArgs!!.valueList[0].text)
        } else {
            listOf(propName.text)
        }
    } else if (this is DTOAliasGroup) {
        emptyList()
    } else {
        println("Only support find path for prop, currently finding prop for <${this::class.simpleName}>")
        emptyList()
    }

    if (!haveUpper || parent.parent.parent is DTODto) {
        return propName
    }
    return upper.propPath() + propName
}

inline fun <reified P> PsiElement.parent(): P {
    return parent as P
}

fun DTODto.classFile(): VirtualFile? {
    val dtoName = dtoName.text
    // 获取GenerateSource源码路径
    val generateRoot = generateRoot(this) ?: return null
    val fileManager = VirtualFileManager.getInstance()
    val export = virtualFile.toPsiFile(project)?.getChildOfType<DTOExport>()
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

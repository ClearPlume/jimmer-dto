package net.fallingangel.jimmerdto.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import net.fallingangel.jimmerdto.DTOFileType
import net.fallingangel.jimmerdto.DTOLanguage
import net.fallingangel.jimmerdto.exception.UnsupportedLanguageException
import net.fallingangel.jimmerdto.lsi.*
import net.fallingangel.jimmerdto.psi.element.DTOExportStatement
import net.fallingangel.jimmerdto.util.findChildNullable
import net.fallingangel.jimmerdto.util.findChildren
import net.fallingangel.jimmerdto.util.propPath
import org.jetbrains.kotlin.idea.KotlinLanguage

class DTOFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, DTOLanguage) {
    private val implicitPackage: String
        get() {
            val virtualFile = originalFile.virtualFile
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

    val projectLanguage: Language
        get() {
            val fileIndex = ProjectRootManager.getInstance(project).fileIndex
            val root = fileIndex.getContentRootForFile(originalFile.virtualFile) ?: throw IllegalStateException("Source root is null")
            return when {
                root.findChild("java") != null -> JavaLanguage.INSTANCE
                root.findChild("kotlin") != null -> KotlinLanguage.INSTANCE
                else -> throw UnsupportedLanguageException(root.children.joinToString(prefix = "[", postfix = "]", transform = VirtualFile::getName))
            }
        }

    val `package`: String
        get() {
            val export = findChildNullable<DTOExportStatement>("/dtoFile/exportStatement")
            return export?.export?.run { "$simpleName.dto" } ?: "$implicitPackage.dto"
        }

    val qualifiedEntity: String
        get() {
            val export = findChildNullable<DTOExportStatement>("/dtoFile/exportStatement")
            return export?.export?.value ?: "$implicitPackage.${originalFile.virtualFile.nameWithoutExtension}"
        }

    val clazz: LClass<*>
        get() = CachedValuesManager.getCachedValue(this, CACHED_CLASS_KEY) {
            val processor = LanguageProcessor.analyze(this)
            processor.clearTypeCache()
            val clazz = processor.clazz(this)

            val classDependencies = mutableSetOf<PsiElement>()
            clazz.collectPsiElements(classDependencies)

            CachedValueProvider.Result.create(
                clazz,
                DumbService.getInstance(project).modificationTracker,
                ProjectRootModificationTracker.getInstance(project),
                *classDependencies.toTypedArray(),
            )
        }

    val dtos: List<String>
        get() = CachedValuesManager.getCachedValue(this, CACHED_DTO_KEY) {
            CachedValueProvider.Result.create(
                findChildren<PsiElement>("/dtoFile/dto/Identifier").map(PsiElement::getText),
                DumbService.getInstance(project).modificationTracker,
                ProjectRootModificationTracker.getInstance(project),
                this,
            )
        }

    override fun getFileType() = DTOFileType.INSTANCE

    override fun toString() = "JimmerDTO File"

    /**
     * 查找属性所在类定义
     *
     * @param lastPathExcluded 是否将最后一个节点抛弃
     *
     * @return prop containing LClass
     */
    fun findOwnClass(prop: DTOPositiveProp, childPath: List<String> = emptyList(), lastPathExcluded: Boolean = childPath.isEmpty()): LClass<*> {
        val tokens = prop.propPath() + childPath
        return if (tokens.size == 1 && lastPathExcluded) {
            clazz
        } else {
            val type = clazz.findProperty(tokens.dropLast(if (lastPathExcluded) 1 else 0)).type
            if (type is LType.CollectionType) {
                type.elementType as LClass<*>
            } else {
                type as LClass<*>
            }
        }
    }

    /**
     * 查找属性对应字段定义
     */
    fun findProperty(prop: DTOPositiveProp): LProperty<*> {
        return findOwnClass(prop).properties.first { it.name == prop.propName.name }
    }

    /**
     * 查找属性子级属性
     */
    fun findPropertyChildren(
        prop: DTOPositiveProp,
        childPath: List<String> = emptyList(),
        lastPathExcluded: Boolean = childPath.isEmpty(),
    ): List<LProperty<*>> {
        return findOwnClass(prop, childPath, lastPathExcluded).properties
    }

    companion object {
        private val CACHED_CLASS_KEY = Key<CachedValue<LClass<*>>>("DTO_FILE_CACHED_CLASS")
        private val CACHED_DTO_KEY = Key<CachedValue<List<String>>>("DTO_FILE_CACHED_DTO")
    }
}

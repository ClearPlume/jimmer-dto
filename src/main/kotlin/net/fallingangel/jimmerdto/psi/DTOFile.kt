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
import net.fallingangel.jimmerdto.util.qualified
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType

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
            val export = getChildOfType<DTOExportStatement>()
            val `package` = export?.getChildOfType<DTOPackageStatement>()
            return `package`?.qualified
                ?: export?.qualified?.substringBeforeLast('.')?.let { "$it.dto" }
                ?: "$implicitPackage.dto"
        }

    val qualifiedEntity: String
        get() {
            val export = getChildOfType<DTOExportStatement>()
            return export?.qualified ?: "$implicitPackage.${originalFile.virtualFile.nameWithoutExtension}"
        }

    override fun getFileType() = DTOFileType.INSTANCE

    override fun toString() = "JimmerDTO File"

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

    /**
     * 查找路径属性所属LClass
     * @param tokens user.files.name
     * @return files LClass
     */
    fun findClass(tokens: List<String>): LClass<*> {
        return if (tokens.size == 1) {
            clazz
        } else {
            val type = clazz.findProperty(tokens.dropLast(1)).type
            if (type is LType.CollectionType) {
                type.elementType as LClass<*>
            } else {
                type as LClass<*>
            }
        }
    }

    /**
     * 依据路径查找属性
     * @param tokens user.files.name
     */
    fun findProperty(tokens: List<String>): LProperty<*> {
        return clazz.findProperty(tokens)
    }

    companion object {
        private val CACHED_CLASS_KEY = Key<CachedValue<LClass<*>>>("DTO_FILE_CACHED_CLASS")
    }
}

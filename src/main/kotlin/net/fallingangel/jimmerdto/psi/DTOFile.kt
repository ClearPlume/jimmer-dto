package net.fallingangel.jimmerdto.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.openapi.util.Key
import com.intellij.psi.FileViewProvider
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import net.fallingangel.jimmerdto.DTOFileType
import net.fallingangel.jimmerdto.DTOLanguage
import net.fallingangel.jimmerdto.exception.UnsupportedLanguageException
import net.fallingangel.jimmerdto.lsi.LClass
import net.fallingangel.jimmerdto.lsi.LanguageProcessor
import net.fallingangel.jimmerdto.lsi.annotation.hasAnnotation
import net.fallingangel.jimmerdto.psi.element.DTOAlias
import net.fallingangel.jimmerdto.psi.element.DTODtoName
import net.fallingangel.jimmerdto.psi.element.DTOExportStatement
import net.fallingangel.jimmerdto.psi.element.DTOImportStatement
import net.fallingangel.jimmerdto.util.contentRoot
import net.fallingangel.jimmerdto.util.findChildNullable
import net.fallingangel.jimmerdto.util.findChildren
import net.fallingangel.jimmerdto.util.notification
import org.babyfish.jimmer.sql.Entity
import org.jetbrains.kotlin.idea.KotlinLanguage

class DTOFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, DTOLanguage) {
    private val implicitPackage: String
        get() {
            val virtualFile = originalFile.virtualFile
            val root = contentRoot?.path ?: throw IllegalStateException("Source root is null")
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
            val exportLine = Regex("""export\s+\w+(\s*\.\s*\w+)*""").find(text)?.value
            val entity = exportLine?.let {
                generateSequence(Regex("""\w+""").find(it, 6), MatchResult::next)
                        .map(MatchResult::value)
                        .joinToString(separator = ".")
            }
                ?: "$implicitPackage.${originalFile.virtualFile.nameWithoutExtension}"

            val entityClass = JavaPsiFacade.getInstance(project).findClass(entity, ProjectScope.getContentScope(project))
            entityClass ?: run {
                project.notification("Can't retrieve the entity `$entity` DTO correspondence, please check!", NotificationType.ERROR)
                throw IllegalStateException("Entity class $entity is null")
            }

            return when {
                entityClass.language is JavaLanguage -> JavaLanguage.INSTANCE
                entityClass.language is KotlinLanguage -> KotlinLanguage.INSTANCE
                else -> throw UnsupportedLanguageException("Unsupported language ${entityClass.language}")
            }
        }

    val export: DTOExportStatement?
        get() = findChildNullable<DTOExportStatement>("/dtoFile/exportStatement")

    val hasExport: Boolean
        get() = export != null

    val `package`: String
        get() = export?.`package`?.value ?: export?.export?.let { it.`package` + ".dto" } ?: "$implicitPackage.dto"

    val importStatements: List<DTOImportStatement>
        get() = findChildren<DTOImportStatement>("/dtoFile/importStatement")

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

            if (export == null) {
                CachedValueProvider.Result.create(
                    clazz,
                    DumbService.getInstance(project).modificationTracker,
                    ProjectRootModificationTracker.getInstance(project),
                    *classDependencies.toTypedArray(),
                )
            } else {
                CachedValueProvider.Result.create(
                    clazz,
                    DumbService.getInstance(project).modificationTracker,
                    ProjectRootModificationTracker.getInstance(project),
                    *classDependencies.toTypedArray(),
                    export,
                )
            }
        }

    val classIsEntity: Boolean
        get() = clazz.hasAnnotation(Entity::class)

    val dtos: List<String>
        get() = CachedValuesManager.getCachedValue(this, CACHED_DTO_KEY) {
            CachedValueProvider.Result.create(
                findChildren<DTODtoName>("/dtoFile/dto/dtoName").map(DTODtoName::value),
                DumbService.getInstance(project).modificationTracker,
                ProjectRootModificationTracker.getInstance(project),
                this,
            )
        }

    val imports: List<Pair<String, PsiClass>>
        get() {
            val facade = JavaPsiFacade.getInstance(project)
            val scope = ProjectScope.getAllScope(project)

            val singleImports = importStatements
                    .filter { it.groupedImport == null && it.alias == null }
                    .mapNotNull { facade.findClass(it.qualifiedName.value, scope) }
                    .map { it.name!! to it }

            val groupedImports = importStatements
                    .filter { it.groupedImport != null }
                    .map { import -> import.qualifiedName.value to import.groupedImport!!.types.filter { it.alias == null } }
                    .flatMap { (`package`, subTypes) -> subTypes.map { `package` to it } }
                    .mapNotNull { (`package`, subType) ->
                        val type = subType.type.value
                        val qualified = "$`package`.$type"
                        facade.findClass(qualified, scope)?.let { type to it }
                    }
            return singleImports + groupedImports
        }

    val imported: Map<String, PsiClass>
        get() = CachedValuesManager.getCachedValue(this, CACHED_IMPORT_KEY) {
            CachedValueProvider.Result.create(
                imports.toMap(),
                DumbService.getInstance(project).modificationTracker,
                ProjectRootModificationTracker.getInstance(project),
                this,
            )
        }

    val alias: List<Pair<DTOAlias, PsiClass>>
        get() {
            val facade = JavaPsiFacade.getInstance(project)
            val scope = ProjectScope.getAllScope(project)

            val aliasImports = importStatements
                    .filter { it.alias != null }
                    .mapNotNull { facade.findClass(it.qualifiedName.value, scope)?.let { clazz -> it.alias!! to clazz } }

            val groupedImports = importStatements
                    .filter { it.groupedImport != null }
                    .map { import -> import.qualifiedName.value to import.groupedImport!!.types.filter { it.alias != null } }
                    .flatMap { (`package`, subTypes) -> subTypes.map { `package` to it } }
                    .mapNotNull { (`package`, subType) ->
                        val type = subType.type.value
                        val qualified = "$`package`.$type"
                        facade.findClass(qualified, scope)?.let { subType.alias!! to it }
                    }
            return aliasImports + groupedImports
        }

    val importedAlias: Map<String, Pair<DTOAlias, PsiClass>>
        get() = CachedValuesManager.getCachedValue(this, CACHED_IMPORT_ALIAS_KEY) {
            CachedValueProvider.Result.create(
                alias.associate { (alias, clazz) ->
                    alias.value to (alias to clazz)
                },
                DumbService.getInstance(project).modificationTracker,
                ProjectRootModificationTracker.getInstance(project),
                this,
            )
        }

    override fun getFileType() = DTOFileType.INSTANCE

    override fun toString() = "JimmerDTO File"

    companion object {
        private val CACHED_CLASS_KEY = Key<CachedValue<LClass<*>>>("DTO_FILE_CACHED_CLASS")
        private val CACHED_DTO_KEY = Key<CachedValue<List<String>>>("DTO_FILE_CACHED_DTO")
        private val CACHED_IMPORT_KEY = Key<CachedValue<Map<String, PsiClass>>>("DTO_FILE_CACHED_IMPORT")
        private val CACHED_IMPORT_ALIAS_KEY = Key<CachedValue<Map<String, Pair<DTOAlias, PsiClass>>>>("DTO_FILE_CACHED_IMPORT_ALIAS")
    }
}

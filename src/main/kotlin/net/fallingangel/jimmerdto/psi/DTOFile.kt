package net.fallingangel.jimmerdto.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import net.fallingangel.jimmerdto.core.DTOFileType
import net.fallingangel.jimmerdto.core.DTOLanguage
import net.fallingangel.jimmerdto.lsi.*
import net.fallingangel.jimmerdto.project.ProjectSyncTracker
import net.fallingangel.jimmerdto.psi.element.*
import net.fallingangel.jimmerdto.psi.resolve.ImportEntry
import net.fallingangel.jimmerdto.psi.resolve.Resolution
import net.fallingangel.jimmerdto.util.*

class DTOFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, DTOLanguage) {
    private val implicitPackage: String
        get() {
            val file = originalFile.virtualFile ?: return ""
            val dir = file.parent ?: return ""
            val root = ProjectFileIndex.getInstance(project).getSourceRootForFile(file) ?: return ""
            return VfsUtilCore.getRelativePath(dir, root, '.') ?: ""
        }

    val export: DTOExportStatement?
        get() = findChildNullable("/dtoFile/exportStatement")

    val hasExport: Boolean
        get() = export != null

    val entityPackage: String
        get() = export?.export?.`package` ?: implicitPackage

    val `package`: String
        get() = export?.`package`?.value ?: export?.export?.let { it.`package` + ".dto" } ?: "$implicitPackage.dto"

    val importStatements: List<DTOImportStatement>
        get() = findChildren("/dtoFile/importStatement")

    val qualifiedEntity: String
        get() = export?.export?.value ?: buildString {
            if (implicitPackage.isNotEmpty()) {
                append("$implicitPackage.")
            }
            append(originalFile.virtualFile.nameWithoutExtension)
        }

    val precompiler: Precompiler
        get() = compiling(this) { precompiler } ?: missing("module")

    val clazz: LClass?
        get() = CachedValuesManager.getCachedValue(this, CACHED_CLASS_KEY) {
            val dependencies = mutableSetOf(
                this,
                DumbService.getInstance(project).modificationTracker,
                ProjectRootModificationTracker.getInstance(project),
                ProjectSyncTracker.getInstance(project),
            )

            fun unresolved() = CachedValueProvider.Result.create<LClass>(
                null,
                dependencies + PsiModificationTracker.MODIFICATION_COUNT,
            )

            val clazz = LName.fromFqn(qualifiedEntity).psiClass() ?: return@getCachedValue unresolved()
            val entity = process(clazz) { lClass() } ?: return@getCachedValue unresolved()
            entity.collectDependencyItems(dependencies)
            CachedValueProvider.Result.create(entity, dependencies)
        }

    val dtos: List<String>
        get() = CachedValuesManager.getCachedValue(this, CACHED_DTO_KEY) {
            CachedValueProvider.Result.create(
                findChildren<DTODtoName>("/dtoFile/dto/dtoName").map(DTODtoName::value),
                DumbService.getInstance(project).modificationTracker,
                ProjectRootModificationTracker.getInstance(project),
                this,
            )
        }

    val importIndex: Map<String, List<ImportEntry>>
        get() = CachedValuesManager.getCachedValue(this, CACHED_IMPORTS_KEY) {
            val imports = importStatements.flatMap(DTOImportStatement::importEntries).groupBy(ImportEntry::simpleName)
            CachedValueProvider.Result.create(imports, this)
        }

    val removableImportEntries: List<ImportEntry>
        get() {
            val usedTypes = findChildren<DTOQualifiedName>("//qualifiedName")
                .asSequence()
                .filter { it.initialSpace is Resolution.Space.GlobalWithImports || it.initialSpace is Resolution.Space.Subtypes }
                .mapNotNull {
                    when (val target = it.parts.firstOrNull()?.target) {
                        is Resolution.Target.Type, is Resolution.Target.Alias -> target.source
                        else -> null
                    }
                }
                .toList()

            return importStatements
                .flatMap(DTOImportStatement::importEntries)
                .groupBy(ImportEntry::simpleName)
                .values
                .mapNotNull(List<ImportEntry>::singleOrNull)
                .filter { entry ->
                    val importedType = entry.target ?: return@filter false
                    usedTypes.none { importedType.equivalentTo(it) }
                }
        }

    fun addImport(lName: LName) {
        if (lName.pkg == entityPackage) return

        val import = project.createImport(lName.fqName)
        val root = findChild<PsiElement>("/dtoFile")
        val export = export

        if (importStatements.isEmpty()) {
            if (export == null) {
                val inserted = root.addBefore(import, findChild("/dtoFile/dto"))
                CodeStyleManager.getInstance(project).reformatRange(
                    root,
                    0,
                    inserted.textRange.endOffset,
                )
            } else {
                val inserted = root.addAfter(import, export)
                CodeStyleManager.getInstance(project).reformatRange(
                    root,
                    export.textRange.startOffset,
                    inserted.textRange.endOffset,
                )
            }
        } else {
            val inserted = root.addAfter(import, importStatements.last())
            CodeStyleManager.getInstance(project).reformatRange(
                root,
                importStatements.last().textRange.startOffset,
                inserted.textRange.endOffset,
            )
        }
    }

    override fun getFileType() = DTOFileType.INSTANCE

    override fun toString() = "JimmerDTO File"

    companion object {
        private val CACHED_CLASS_KEY = Key<CachedValue<LClass?>>("DTO_FILE_CACHED_CLASS")
        private val CACHED_DTO_KEY = Key<CachedValue<List<String>>>("DTO_FILE_CACHED_DTO")
        private val CACHED_IMPORTS_KEY = Key<CachedValue<Map<String, List<ImportEntry>>>>("DTO_FILE_IMPORTMAP")
    }
}

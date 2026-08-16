package net.fallingangel.jimmerdto.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.FileViewProvider
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import net.fallingangel.jimmerdto.core.DTOFileType
import net.fallingangel.jimmerdto.core.DTOLanguage
import net.fallingangel.jimmerdto.lsi.LClass
import net.fallingangel.jimmerdto.lsi.process
import net.fallingangel.jimmerdto.project.ProjectSyncTracker
import net.fallingangel.jimmerdto.psi.element.DTODtoName
import net.fallingangel.jimmerdto.psi.element.DTOExportStatement
import net.fallingangel.jimmerdto.psi.element.DTOImportStatement
import net.fallingangel.jimmerdto.psi.element.DTOQualifiedName
import net.fallingangel.jimmerdto.psi.resolve.ImportEntry
import net.fallingangel.jimmerdto.psi.resolve.Resolution
import net.fallingangel.jimmerdto.util.findChildNullable
import net.fallingangel.jimmerdto.util.findChildren
import net.fallingangel.jimmerdto.util.psiClass

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

    val clazz: LClass?
        get() = CachedValuesManager.getCachedValue(this, CACHED_CLASS_KEY) {
            val clazz = psiClass(qualifiedEntity) ?: return@getCachedValue null
            val entity = process(clazz) { lClass() } ?: return@getCachedValue null

            val classDependencies = mutableSetOf<Any>()
            entity.collectDependencyItems(classDependencies)

            CachedValueProvider.Result.create(
                entity,
                buildList {
                    add(DumbService.getInstance(project).modificationTracker)
                    add(ProjectRootModificationTracker.getInstance(project))
                    addAll(classDependencies)
                    export?.let(::add)
                    add(ProjectSyncTracker.getInstance(project))
                },
            )
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
            val imports = mutableMapOf<String, MutableList<ImportEntry>>()

            importStatements.forEach { import ->
                val groupedImport = import.groupedImport
                if (groupedImport != null) {
                    val qualified = import.qualifiedName.value
                    groupedImport.types.forEach {
                        imports.computeIfAbsent(it.simpleName) { mutableListOf() }.add(ImportEntry("$qualified.${it.type.value}", it.alias))
                    }
                } else {
                    imports.computeIfAbsent(import.simpleName) { mutableListOf() }.add(ImportEntry(import.qualifiedName.value, import.alias))
                }
            }

            CachedValueProvider.Result.create(imports, this)
        }
    val usedTypeNames: Set<String>
        get() = findChildren<DTOQualifiedName>("//qualifiedName")
            .filter { it.initialSpace is Resolution.Space.GlobalWithImports }
            .map { it.parts.first().part }
            .toSet()

    override fun getFileType() = DTOFileType.INSTANCE

    override fun toString() = "JimmerDTO File"

    companion object {
        private val CACHED_CLASS_KEY = Key<CachedValue<LClass?>>("DTO_FILE_CACHED_CLASS")
        private val CACHED_DTO_KEY = Key<CachedValue<List<String>>>("DTO_FILE_CACHED_DTO")
        private val CACHED_IMPORTS_KEY = Key<CachedValue<Map<String, List<ImportEntry>>>>("DTO_FILE_IMPORTMAP")
    }
}

package net.fallingangel.jimmerdto.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import net.fallingangel.jimmerdto.DTOFileType
import net.fallingangel.jimmerdto.DTOLanguage
import net.fallingangel.jimmerdto.lsi.LClass
import net.fallingangel.jimmerdto.lsi.process
import net.fallingangel.jimmerdto.psi.element.DTODtoName
import net.fallingangel.jimmerdto.psi.element.DTOExportStatement
import net.fallingangel.jimmerdto.psi.element.DTOImportStatement
import net.fallingangel.jimmerdto.util.findChildNullable
import net.fallingangel.jimmerdto.util.findChildren
import net.fallingangel.jimmerdto.util.psiClass

class DTOFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, DTOLanguage) {
    private val implicitPackage: String
        get() {
            val dir = originalFile.virtualFile?.parent ?: return ""
            val root = generateSequence(dir, VirtualFile::getParent)
                .firstOrNull { it.name == "dto" } ?: return ""
            return VfsUtilCore.getRelativePath(dir, root, '.') ?: ""
        }

    val export: DTOExportStatement?
        get() = findChildNullable<DTOExportStatement>("/dtoFile/exportStatement")

    val hasExport: Boolean
        get() = export != null

    val entityPackage: String
        get() = export?.export?.`package` ?: implicitPackage

    val `package`: String
        get() = export?.`package`?.value ?: export?.export?.let { it.`package` + ".dto" } ?: "$implicitPackage.dto"

    val importStatements: List<DTOImportStatement>
        get() = findChildren<DTOImportStatement>("/dtoFile/importStatement")

    val qualifiedEntity: String
        get() {
            val export = findChildNullable<DTOExportStatement>("/dtoFile/exportStatement")
            return export?.export?.value ?: "$implicitPackage.${originalFile.virtualFile.nameWithoutExtension}"
        }

    val clazz: LClass?
        get() = CachedValuesManager.getCachedValue(this, CACHED_CLASS_KEY) {
            val clazz = project.psiClass(qualifiedEntity) ?: return@getCachedValue null
            val entity = process(clazz) { lClass() } ?: return@getCachedValue null

            val classDependencies = mutableSetOf<PsiElement>()
            entity.collectPsiElements(classDependencies)

            CachedValueProvider.Result.create(
                entity,
                buildList {
                    add(DumbService.getInstance(project).modificationTracker)
                    add(ProjectRootModificationTracker.getInstance(project))
                    addAll(classDependencies)
                    export?.let(::add)
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

    val importIndex: Map<String, List<String>>
        get() = CachedValuesManager.getCachedValue(this, CACHED_IMPORTS_KEY) {
            val imports = mutableMapOf<String, MutableList<String>>()

            importStatements.forEach { import ->
                val groupedImport = import.groupedImport
                if (groupedImport != null) {
                    val qualified = import.qualifiedName.value
                    groupedImport.types.forEach {
                        val name = it.alias?.value ?: it.type.value
                        imports.computeIfAbsent(name) { mutableListOf() }.add("$qualified.${it.type.value}")
                    }
                } else {
                    val name = import.alias?.value ?: import.qualifiedName.simpleName
                    imports.computeIfAbsent(name) { mutableListOf() }.add(import.qualifiedName.value)
                }
            }

            CachedValueProvider.Result.create(imports, this)
        }

    override fun getFileType() = DTOFileType.INSTANCE

    override fun toString() = "JimmerDTO File"

    companion object {
        private val CACHED_CLASS_KEY = Key<CachedValue<LClass?>>("DTO_FILE_CACHED_CLASS")
        private val CACHED_DTO_KEY = Key<CachedValue<List<String>>>("DTO_FILE_CACHED_DTO")
        private val CACHED_IMPORTS_KEY = Key<CachedValue<Map<String, List<String>>>>("DTO_FILE_IMPORTMAP")
    }
}

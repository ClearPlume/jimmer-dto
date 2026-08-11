package net.fallingangel.jimmerdto.project.sourceroot

import com.intellij.ide.actions.CreateDirectoryCompletionContributor
import com.intellij.ide.actions.CreateDirectoryCompletionContributor.Variant
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiDirectory

class CreateDirectoryCompletionContributor : CreateDirectoryCompletionContributor {
    override fun getDescription(): String {
        return "Jimmer DTO source roots"
    }

    override fun getVariants(directory: PsiDirectory): Collection<Variant> {
        val module = ProjectFileIndex.getInstance(directory.project)
            .getModuleForFile(directory.virtualFile)
            ?: return emptyList()

        return ModuleRootManager.getInstance(module).contentEntries
            .flatMap { it.getSourceFolders(setOf(DtoSourceRootType.SOURCE, DtoSourceRootType.TEST_SOURCE)) }
            .mapNotNull { folder ->
                folder.file?.let { return@mapNotNull null }
                val path = VfsUtilCore.urlToPath(folder.url)
                Variant(path, folder.rootType)
            }
    }
}
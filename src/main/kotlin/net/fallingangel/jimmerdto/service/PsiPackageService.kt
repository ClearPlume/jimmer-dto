package net.fallingangel.jimmerdto.service

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import net.fallingangel.jimmerdto.util.isFile
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.KtFile

@Service(Service.Level.PROJECT)
class PsiPackageService(private val project: Project) {
    private val projectScope = ProjectScope.getProjectScope(project)

    private val javaPackagesCache: CachedValue<List<List<String>>>
    private val kotlinPackagesCache: CachedValue<List<List<String>>>

    init {
        javaPackagesCache = CachedValuesManager.getManager(project)
                .createCachedValue(
                    {
                        val packages = findJavaPackages()
                        CachedValueProvider.Result(packages, PsiModificationTracker.MODIFICATION_COUNT)
                    },
                    false
                )
        kotlinPackagesCache = CachedValuesManager.getManager(project)
                .createCachedValue(
                    {
                        val packages = findKotlinPackages()
                        CachedValueProvider.Result(packages, PsiModificationTracker.MODIFICATION_COUNT)
                    },
                    false
                )
    }

    private fun findJavaPackages(): List<List<String>> {
        return FileTypeIndex.getFiles(JavaFileType.INSTANCE, projectScope)
                .asSequence()
                .filter { it.isFile }
                .mapNotNull { it.toPsiFile(project) as? PsiJavaFile }
                .map { it.packageName }
                .distinct()
                .map { it.split('.') }
                .toList()
    }

    private fun findKotlinPackages(): List<List<String>> {
        return FileTypeIndex.getFiles(KotlinFileType.INSTANCE, projectScope)
                .asSequence()
                .filter { it.isFile }
                .mapNotNull { (it.toPsiFile(project) as? KtFile)?.packageDirective?.fqName }
                .filterNot { it.isRoot }
                .map { it.asString() }
                .distinct()
                .map { it.split('.') }
                .toList()
    }

    fun javaPackages(): List<List<String>> = javaPackagesCache.value
    fun kotlinPackages(): List<List<String>> = kotlinPackagesCache.value
}
package net.fallingangel.jimmerdto.service

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import net.fallingangel.jimmerdto.Constant
import net.fallingangel.jimmerdto.cache.LanguageModificationTracker
import net.fallingangel.jimmerdto.util.hasAnnotation
import net.fallingangel.jimmerdto.util.isFile
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile


@Service(Service.Level.PROJECT)
class PsiCacheService(private val project: Project) {
    private val projectScope = ProjectScope.getProjectScope(project)
    private val psiManager = PsiManager.getInstance(project)
    private val javaTracker = LanguageModificationTracker(project, JavaFileType.INSTANCE)
    private val kotlinTracker = LanguageModificationTracker(project, KotlinFileType.INSTANCE)

    private val javaPackagesCache: CachedValue<List<List<String>>>
    private val kotlinPackagesCache: CachedValue<List<List<String>>>

    private val javaEntitiesCache: CachedValue<List<PsiClass>>
    private val kotlinEntitiesCache: CachedValue<List<KtClass>>

    init {
        javaPackagesCache = CachedValuesManager.getManager(project)
                .createCachedValue(
                    {
                        val packages = findJavaPackages()
                        CachedValueProvider.Result(packages, javaTracker)
                    },
                    false
                )
        kotlinPackagesCache = CachedValuesManager.getManager(project)
                .createCachedValue(
                    {
                        val packages = findKotlinPackages()
                        CachedValueProvider.Result(packages, kotlinTracker)
                    },
                    false
                )

        javaEntitiesCache = CachedValuesManager.getManager(project)
                .createCachedValue(
                    {
                        val packages = findJavaEntities()
                        CachedValueProvider.Result(packages, javaTracker)
                    },
                    false
                )
        kotlinEntitiesCache = CachedValuesManager.getManager(project)
                .createCachedValue(
                    {
                        val packages = findKotlinEntities()
                        CachedValueProvider.Result(packages, kotlinTracker)
                    },
                    false
                )
    }

    val javaPackages: List<List<String>> = javaPackagesCache.value

    val kotlinPackages: List<List<String>> = kotlinPackagesCache.value

    val javaEntities: List<PsiClass> = javaEntitiesCache.value

    val kotlinEntities: List<KtClass> = kotlinEntitiesCache.value

    fun javaEntitiesByPackage(`package`: String): List<PsiClass> {
        return javaEntities.filter { it.qualifiedName!!.substringBeforeLast('.') == `package` }
    }

    fun kotlinEntitiesByPackage(`package`: String): List<KtClass> {
        return kotlinEntities.filter { it.containingKtFile.packageFqName.asString() == `package` }
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

    private fun findJavaEntities(): List<PsiClass> {
        return FileTypeIndex.getFiles(JavaFileType.INSTANCE, projectScope)
                .mapNotNull { psiManager.findFile(it) as? PsiJavaFile }
                .map { javaFile -> javaFile.classes.filter { it.isInterface && it.hasAnnotation(Constant.Annotation.ENTITY) } }
                .flatten()
    }

    private fun findKotlinEntities(): List<KtClass> {
        return FileTypeIndex.getFiles(KotlinFileType.INSTANCE, projectScope)
                .mapNotNull { psiManager.findFile(it) as? KtFile }
                .map { it.declarations.filterIsInstance<KtClass>() }
                .flatten()
                .filter { it.isInterface() && it.hasAnnotation(Constant.Annotation.ENTITY) }
    }
}
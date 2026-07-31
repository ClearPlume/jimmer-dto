package net.fallingangel.jimmerdto.refactor

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import com.intellij.util.indexing.FileBasedIndex
import net.fallingangel.jimmerdto.index.DTO_ENTITY_INDEX
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.KtClass

class EntityRenameProcessor : RenamePsiElementProcessor() {
    override fun canProcessElement(element: PsiElement) = element is PsiClass || element is KtClass

    override fun prepareRenaming(element: PsiElement, newName: String, allRenames: MutableMap<PsiElement, String>, scope: SearchScope) {
        val entityName = when (element) {
            is KtClass -> if (element.isInterface() && element.annotationEntries.any { it.shortName?.asString() == "Entity" }) element.fqName?.asString() else null
            is PsiClass -> if (element.isInterface && element.annotations.any { it.qualifiedName == "org.babyfish.jimmer.sql.Entity" }) element.qualifiedName else null
            else -> null
        }

        entityName ?: return
        val project = element.project

        FileBasedIndex.getInstance().processValues(
            DTO_ENTITY_INDEX,
            entityName,
            null,
            { file, hasExport ->
                if (!hasExport) {
                    file.toPsiFile(project)?.let { allRenames[it] = "$newName.dto" }
                }
                true
            },
            GlobalSearchScope.projectScope(project),
        )
    }
}
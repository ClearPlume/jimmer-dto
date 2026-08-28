package net.fallingangel.jimmerdto.refactor

import com.intellij.openapi.application.readAction
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import com.intellij.util.indexing.FileBasedIndex
import net.fallingangel.jimmerdto.index.DTO_ENTITY_INDEX
import net.fallingangel.jimmerdto.lsi.jimmer.isEntity
import net.fallingangel.jimmerdto.lsi.process
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.KtClass

class EntityRenameProcessor : RenamePsiElementProcessor() {
    override fun canProcessElement(element: PsiElement) = element is PsiClass || element is KtClass

    override fun prepareRenaming(element: PsiElement, newName: String, allRenames: MutableMap<PsiElement, String>, scope: SearchScope) {
        val project = element.project

        val entityName = runWithModalProgressBlocking(project, "Analyzing Entity") {
            readAction {
                process(element) {
                    takeIf { isEntity() }?.className()?.fqName
                }
            }
        } ?: return

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

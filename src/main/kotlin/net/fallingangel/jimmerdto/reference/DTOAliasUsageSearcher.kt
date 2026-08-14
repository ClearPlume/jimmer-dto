package net.fallingangel.jimmerdto.reference

import com.intellij.find.findUsages.CustomUsageSearcher
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.openapi.application.ReadAction
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.usageView.UsageInfo
import com.intellij.usages.Usage
import com.intellij.usages.UsageInfo2UsageAdapter
import com.intellij.util.Processor
import com.intellij.util.indexing.FileBasedIndex
import net.fallingangel.jimmerdto.core.DTOFileType
import net.fallingangel.jimmerdto.index.DTO_IMPORT_ALIAS_INDEX
import net.fallingangel.jimmerdto.lsi.LKind
import net.fallingangel.jimmerdto.lsi.process
import net.fallingangel.jimmerdto.psi.DTOFile
import org.jetbrains.kotlin.idea.core.util.toPsiFile

class DTOAliasUsageSearcher : CustomUsageSearcher() {
    override fun processElementUsages(element: PsiElement, processor: Processor<in Usage>, options: FindUsagesOptions) {
        ReadAction.run<Exception> {
            val qualifiedName = process(element) {
                val kind = kind()
                if (kind in setOf(LKind.Class, LKind.Interface, LKind.Annotation, LKind.Enum)) {
                    classQualifiedName()
                } else {
                    null
                }
            } ?: return@run
            val project = element.project
            val scope = when (val scope = options.searchScope) {
                is GlobalSearchScope -> GlobalSearchScope.getScopeRestrictedByFileTypes(scope, DTOFileType.INSTANCE)
                else -> project.dtoScope
            }

            FileBasedIndex.getInstance().getFilesWithKey(
                DTO_IMPORT_ALIAS_INDEX,
                setOf(qualifiedName),
                { virtualFile ->
                    val file = virtualFile.toPsiFile(project) as? DTOFile ?: return@getFilesWithKey true
                    file.importIndex.values
                        .flatten()
                        .filter { it.qualifiedName == qualifiedName }
                        .forEach {
                            if (it.alias != null) {
                                processor.process(UsageInfo2UsageAdapter(UsageInfo(it.alias)))
                                for (reference in ReferencesSearch.search(it.alias, scope)) {
                                    processor.process(UsageInfo2UsageAdapter(UsageInfo(reference)))
                                }
                            }
                        }
                    true
                },
                scope,
            )
        }
    }
}

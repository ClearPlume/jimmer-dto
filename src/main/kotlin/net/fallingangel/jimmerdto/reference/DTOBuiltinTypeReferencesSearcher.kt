package net.fallingangel.jimmerdto.reference

import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.Processor
import net.fallingangel.jimmerdto.core.DTOFileType
import net.fallingangel.jimmerdto.lsi.LKind
import net.fallingangel.jimmerdto.lsi.process

class DTOBuiltinTypeReferencesSearcher : QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>(true) {
    override fun processQuery(queryParameters: ReferencesSearch.SearchParameters, consumer: Processor<in PsiReference>) {
        val element = queryParameters.elementToSearch
        val aliases = process(element) {
            val kind = kind()
            if (kind in setOf(LKind.Class, LKind.Interface)) {
                builtinAliases()
            } else {
                emptyList()
            }
        } ?: return

        val scope = queryParameters.effectiveSearchScope.intersectWith(
            GlobalSearchScope.getScopeRestrictedByFileTypes(
                GlobalSearchScope.allScope(element.project),
                DTOFileType.INSTANCE,
            )
        )

        aliases.forEach { alias ->
            queryParameters.optimizer.searchWord(
                alias,
                scope,
                UsageSearchContext.IN_CODE,
                true,
                element,
            )
        }
    }
}
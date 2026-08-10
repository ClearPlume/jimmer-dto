package net.fallingangel.jimmerdto.reference

import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchRequestCollector
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.search.searches.MethodReferencesSearch
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.Processor
import net.fallingangel.jimmerdto.core.DTOFileType
import net.fallingangel.jimmerdto.lsi.jimmer.isReference
import net.fallingangel.jimmerdto.lsi.process

class DTOAssociationIdReferencesSearcher : QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>(true) {
    override fun processQuery(queryParameters: ReferencesSearch.SearchParameters, consumer: Processor<in PsiReference>) {
        contribute(
            queryParameters.elementToSearch,
            queryParameters.optimizer,
            queryParameters.effectiveSearchScope,
        )
    }
}

class DTOAssociationIdMethodReferencesSearcher : QueryExecutorBase<PsiReference, MethodReferencesSearch.SearchParameters>(true) {
    override fun processQuery(queryParameters: MethodReferencesSearch.SearchParameters, consumer: Processor<in PsiReference>) {
        contribute(
            queryParameters.method,
            queryParameters.optimizer,
            queryParameters.effectiveSearchScope,
        )
    }
}

private fun contribute(
    element: PsiElement,
    optimizer: SearchRequestCollector,
    scope: SearchScope,
) {
    val clazz = process(element) { containingClass() } ?: return
    val lClass = process(clazz) { lClass() } ?: return
    val property = lClass.properties.find { it.source?.isEquivalentTo(element) == true } ?: return
    if (!property.isReference) return

    val scope = scope.intersectWith(
        GlobalSearchScope.getScopeRestrictedByFileTypes(
            GlobalSearchScope.allScope(element.project),
            DTOFileType.INSTANCE,
        )
    )

    optimizer.searchWord(
        "${property.name}Id",
        scope,
        UsageSearchContext.IN_CODE,
        true,
        element,
    )
}
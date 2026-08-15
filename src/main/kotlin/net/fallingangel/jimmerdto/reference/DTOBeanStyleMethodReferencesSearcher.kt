package net.fallingangel.jimmerdto.reference

import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.psi.PsiReference
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.search.searches.MethodReferencesSearch
import com.intellij.util.Processor
import net.fallingangel.jimmerdto.lsi.process

class DTOBeanStyleMethodReferencesSearcher : QueryExecutorBase<PsiReference, MethodReferencesSearch.SearchParameters>(true) {
    override fun processQuery(queryParameters: MethodReferencesSearch.SearchParameters, consumer: Processor<in PsiReference>) {
        val element = queryParameters.method
        val clazz = process(element) { containingClass() } ?: return
        val lClass = process(clazz) { lClass() } ?: return
        val property = lClass.properties.find { it.source.isEquivalentTo(element) } ?: return

        if (property.name == element.name) return
        queryParameters.optimizer.searchWord(
            property.name,
            queryParameters.effectiveSearchScope.intersectWith(element.project.dtoScope),
            UsageSearchContext.IN_CODE,
            true,
            element,
        )
    }
}

package net.fallingangel.jimmerdto.reference

import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.psi.PsiReference
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.Processor
import net.fallingangel.jimmerdto.lsi.jimmer.isReference
import net.fallingangel.jimmerdto.lsi.process

class DTOAssociationIdSearchWordContributor : QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>(true) {
    override fun processQuery(queryParameters: ReferencesSearch.SearchParameters, consumer: Processor<in PsiReference>) {
        val element = queryParameters.elementToSearch

        val clazz = process(element) { containingClass() } ?: return
        val lClass = process(clazz) { lClass() } ?: return
        val property = lClass.properties.find { it.source?.isEquivalentTo(element) == true } ?: return
        if (!property.isReference) return

        queryParameters.optimizer.searchWord(
            "${property.name}Id",
            queryParameters.effectiveSearchScope,
            UsageSearchContext.IN_CODE,
            true,
            element,
        )
    }
}

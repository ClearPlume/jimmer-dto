package net.fallingangel.jimmerdto.lsi.annotation

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.lsi.LElement
import net.fallingangel.jimmerdto.lsi.LPsiDependent
import net.fallingangel.jimmerdto.lsi.param.LParam
import net.fallingangel.jimmerdto.lsi.param.LParamOwner

data class LAnnotation<A : PsiElement?>(
    override val name: String,
    val canonicalName: String,
    override val source: A,
    override val params: List<LParam<*>>,
) : LElement, LParamOwner, LPsiDependent {
    override fun collectPsiElements(result: MutableSet<PsiElement>, visited: MutableSet<LPsiDependent>) {
        if (!visited.add(this)) {
            return
        }
        source?.let(result::add)
        params.forEach { it.collectPsiElements(result, visited) }
    }
}
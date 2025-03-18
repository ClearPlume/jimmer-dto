package net.fallingangel.jimmerdto.lsi.param

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.lsi.LClass
import net.fallingangel.jimmerdto.lsi.LElement
import net.fallingangel.jimmerdto.lsi.LPsiDependent
import net.fallingangel.jimmerdto.lsi.LType

data class LParam<P: PsiElement?>(
    override val name: String,
    val type: LType,
    override val source: P,
    ) : LElement, LPsiDependent {
    override fun collectPsiElements(result: MutableSet<PsiElement>, visited: MutableSet<LPsiDependent>) {
        if (!visited.add(this)) {
            return
        }
        source?.let(result::add)
        if (type is LClass<*>) {
            type.collectPsiElements(result, visited)
        } else if (type is LType.EnumType<*, *>) {
            type.collectPsiElements(result, visited)
        }
    }
}
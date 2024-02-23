package net.fallingangel.jimmerdto.reference

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import net.fallingangel.jimmerdto.psi.DTOPositiveProp
import net.fallingangel.jimmerdto.psi.impl.DTOPsiImplUtil
import net.fallingangel.jimmerdto.util.*


class DTOPropReference(private val element: PsiElement, range: TextRange) : PsiReferenceBase<PsiElement>(element, range) {
    override fun resolve(): PsiElement? {
        element as DTOPositiveProp
        val propName = element.name
        println("resolving reference: $propName")
        val entityFile = element.virtualFile.entityFile(element.project) ?: throw IllegalStateException()
        return if (entityFile.isJavaOrKotlin) {
            val clazz = DTOPsiImplUtil.findPsiClass(element)
            clazz.methods().find { it.name == propName }?.originalElement
        } else {
            val clazz = DTOPsiImplUtil.findKtClass(element)
            clazz.properties().find { it.name == propName }?.originalElement
        }
    }
}

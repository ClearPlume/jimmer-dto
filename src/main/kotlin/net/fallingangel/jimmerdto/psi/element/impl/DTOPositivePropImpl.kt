package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.enums.Modifier
import net.fallingangel.jimmerdto.psi.element.*
import net.fallingangel.jimmerdto.util.findChild
import net.fallingangel.jimmerdto.util.findChildNullable
import net.fallingangel.jimmerdto.util.findChildren
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTOPositivePropImpl(node: ASTNode) : ANTLRPsiNode(node), DTOPositiveProp {
    override val annotations: List<DTOAnnotation>
        get() = findChildren("/positiveProp/annotation")

    override val configs: List<DTOPropConfig>
        get() = findChildren("/positiveProp/propConfig")

    override val modifier: Modifier?
        get() = findChildNullable<PsiElement>("/positiveProp/Modifier")?.let { Modifier.valueOf(it.text) }

    override val name: DTOPropName
        get() = findChild("/positiveProp/propName")

    override val flag: DTOPropFlag?
        get() = findChildNullable("/positiveProp/propFlag")

    override val arg: DTOPropArg?
        get() = findChildNullable("/positiveProp/propArg")

    override val body: DTOPropBody?
        get() = findChildNullable("/positiveProp/propBody")

    override val `as`: PsiElement?
        get() = findChildNullable("/positiveProp/'as'")

    override val alias: DTOAlias?
        get() = findChildNullable("/positiveProp/alias")

    override val optional: PsiElement?
        get() = findChildNullable("/positiveProp/'?'")

    override val required: PsiElement?
        get() = findChildNullable("/positiveProp/'!'")

    override val recursive: PsiElement?
        get() = findChildNullable("/positiveProp/'*'")

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitPositiveProp(this)
        } else {
            super.accept(visitor)
        }
    }
}

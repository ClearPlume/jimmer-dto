package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.*
import net.fallingangel.jimmerdto.util.findChild
import net.fallingangel.jimmerdto.util.findChildNullable
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTOPropConfigImpl(node: ASTNode) : ANTLRPsiNode(node), DTOPropConfig {
    override val name: PsiElement
        get() = findChild("/propConfig/PropConfigName")

    override val whereArgs: DTOWhereArgs?
        get() = findChildNullable("/propConfig/whereArgs")

    override val orderByArgs: DTOOrderByArgs?
        get() = findChildNullable("/propConfig/orderByArgs")

    override val qualifiedName: DTOQualifiedName?
        get() = findChildNullable("/propConfig/qualifiedName")

    override val identifier: PsiElement?
        get() = findChildNullable("/propConfig/Identifier")

    override val intPair: DTOIntPair?
        get() = findChildNullable("/propConfig/intPair")

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitPropConfig(this)
        } else {
            super.accept(visitor)
        }
    }
}
package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.util.elementType
import com.intellij.psi.util.prevLeafs
import net.fallingangel.jimmerdto.DTOLanguage
import net.fallingangel.jimmerdto.psi.DTOParser
import net.fallingangel.jimmerdto.psi.element.DTOQualifiedNamePart
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.psi.element.createQualifiedNamePart
import net.fallingangel.jimmerdto.psi.mixin.DTONamedElement
import net.fallingangel.jimmerdto.psi.mixin.impl.DTONamedElementImpl
import net.fallingangel.jimmerdto.util.findChild

class DTOQualifiedNamePartImpl(node: ASTNode) : DTONamedElementImpl(node), DTOQualifiedNamePart {
    override val part: String
        get() = nameIdentifier.text

    override fun getNameIdentifier(): PsiElement {
        return findChild("/qualifiedNamePart/Identifier")
    }

    override fun setName(name: String): DTONamedElement {
        node.treeParent.replaceChild(node, project.createQualifiedNamePart(name).node)
        return this
    }

    override fun resolve(): PsiElement? {
        val value = part
        val `package` = prevLeafs
                .takeWhile { it.elementType !in DTOLanguage.tokenSet(DTOParser.Export, DTOParser.Package, DTOParser.Import) }
                .filter { it.elementType == DTOLanguage.token[DTOParser.Identifier] }
                .toList()
                .asReversed()
                .joinToString(".") { it.text }
        val qualified = if (`package`.isEmpty()) {
            value
        } else {
            "$`package`.$value"
        }
        val psiFacade = JavaPsiFacade.getInstance(project)
        return psiFacade.findClass(qualified, ProjectScope.getAllScope(project)) ?: psiFacade.findPackage(qualified)
    }

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitQualifiedNamePart(this)
        } else {
            super.accept(visitor)
        }
    }
}

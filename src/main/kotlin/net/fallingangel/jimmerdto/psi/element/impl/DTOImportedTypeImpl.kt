package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.elementType
import net.fallingangel.jimmerdto.DTOLanguage
import net.fallingangel.jimmerdto.psi.DTOParser
import net.fallingangel.jimmerdto.psi.element.DTOImportStatement
import net.fallingangel.jimmerdto.psi.element.DTOImportedType
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.psi.element.createImportedType
import net.fallingangel.jimmerdto.psi.mixin.impl.DTONamedElementImpl
import net.fallingangel.jimmerdto.util.findChild
import net.fallingangel.jimmerdto.util.findChildNullable
import net.fallingangel.jimmerdto.util.parent
import net.fallingangel.jimmerdto.util.sibling

class DTOImportedTypeImpl(node: ASTNode) : DTONamedElementImpl(node), DTOImportedType {
    override val type: PsiElement
        get() = nameIdentifier

    override val alias: PsiElement?
        get() = findChildNullable<PsiElement>("/importedType/'as'")
                ?.sibling { it.elementType == DTOLanguage.token[DTOParser.Identifier] }

    override fun getNameIdentifier(): PsiElement {
        return findChild("/importedType/Identifier")
    }

    override fun newNameNode(name: String): ASTNode {
        return project.createImportedType(name).node
    }

    override fun resolve(): PsiElement? {
        val import = parent.parent<DTOImportStatement>()
        val classes = JavaPsiFacade.getInstance(project)
                .findPackage(import.qualifiedName.value)
                ?.classes ?: return null
        return classes.find { it.name == type.text }
    }

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitImportedType(this)
        } else {
            super.accept(visitor)
        }
    }
}

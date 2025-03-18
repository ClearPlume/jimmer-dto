package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.DTOPositiveProp
import net.fallingangel.jimmerdto.psi.element.DTOValue
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.psi.element.createValue
import net.fallingangel.jimmerdto.psi.mixin.impl.DTONamedElementImpl
import net.fallingangel.jimmerdto.util.file
import net.fallingangel.jimmerdto.util.findChild
import net.fallingangel.jimmerdto.util.parent
import net.fallingangel.jimmerdto.util.propPath

class DTOValueImpl(node: ASTNode) : DTONamedElementImpl(node), DTOValue {
    override fun getNameIdentifier(): PsiElement {
        return findChild("/value/Identifier")
    }

    override fun newNameNode(name: String): ASTNode {
        return project.createValue(name).node
    }

    override fun resolve(): PsiElement? {
        val prop = parent.parent<DTOPositiveProp>()
        return file.clazz.propertyOrNull(prop.propPath() + listOf(nameIdentifier.text))?.source
    }

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitValue(this)
        } else {
            super.accept(visitor)
        }
    }
}

package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.search.ProjectScope
import net.fallingangel.jimmerdto.psi.element.DTODtoName
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.psi.element.createDTOName
import net.fallingangel.jimmerdto.psi.mixin.impl.DTONamedElementImpl
import net.fallingangel.jimmerdto.util.file
import net.fallingangel.jimmerdto.util.findChild

class DTODtoNameImpl(node: ASTNode) : DTONamedElementImpl(node), DTODtoName {
    override val value: String
        get() = nameIdentifier.text

    override fun getNameIdentifier(): PsiElement {
        return findChild("/dtoName/Identifier")
    }

    override fun newNameNode(name: String): ASTNode {
        return project.createDTOName(name).node
    }

    override fun resolve(): PsiElement? {
        return JavaPsiFacade.getInstance(project)
                .findClass(
                    "${file.`package`}.$value",
                    ProjectScope.getAllScope(project),
                )
    }

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitDtoName(this)
        } else {
            super.accept(visitor)
        }
    }
}

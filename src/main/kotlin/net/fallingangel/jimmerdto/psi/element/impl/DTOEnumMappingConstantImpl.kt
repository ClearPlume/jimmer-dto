package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.lsi.LType
import net.fallingangel.jimmerdto.psi.element.DTOEnumMappingConstant
import net.fallingangel.jimmerdto.psi.element.DTOPositiveProp
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.psi.element.createEnumMapping
import net.fallingangel.jimmerdto.psi.mixin.impl.DTONamedElementImpl
import net.fallingangel.jimmerdto.util.file
import net.fallingangel.jimmerdto.util.findChild
import net.fallingangel.jimmerdto.util.parent

class DTOEnumMappingConstantImpl(node: ASTNode) : DTONamedElementImpl(node), DTOEnumMappingConstant {
    override val constant: PsiElement
        get() = nameIdentifier

    override fun getNameIdentifier(): PsiElement {
        return findChild("/enumMappingConstant/Identifier")
    }

    override fun newNameNode(name: String): ASTNode {
        return project.createEnumMapping("$name: \"DummyValue\"").constant.node
    }

    override fun resolve(): PsiElement? {
        val name = nameIdentifier.text
        val prop = parent.parent.parent.parent<DTOPositiveProp>()
        val propType = try {
            file.findProperty(prop).actualType
        } catch (_: Exception) {
            return null
        }
        if (propType !is LType.EnumType<*, *>) {
            return null
        }
        return propType.values[name]
    }

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitEnumMappingConstant(this)
        } else {
            super.accept(visitor)
        }
    }
}

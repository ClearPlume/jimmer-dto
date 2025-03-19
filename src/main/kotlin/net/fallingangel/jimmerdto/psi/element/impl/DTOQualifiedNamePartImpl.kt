package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.util.elementType
import com.intellij.psi.util.parentOfType
import com.intellij.psi.util.siblings
import net.fallingangel.jimmerdto.DTOLanguage
import net.fallingangel.jimmerdto.enums.PropConfigName
import net.fallingangel.jimmerdto.psi.DTOParser
import net.fallingangel.jimmerdto.psi.element.*
import net.fallingangel.jimmerdto.psi.mixin.impl.DTONamedElementImpl
import net.fallingangel.jimmerdto.util.file
import net.fallingangel.jimmerdto.util.findChild
import net.fallingangel.jimmerdto.util.propPath

class DTOQualifiedNamePartImpl(node: ASTNode) : DTONamedElementImpl(node), DTOQualifiedNamePart {
    override val part: String
        get() = nameIdentifier.text

    override fun getNameIdentifier(): PsiElement {
        return findChild("/qualifiedNamePart/Identifier")
    }

    override fun newNameNode(name: String): ASTNode {
        return project.createQualifiedNamePart(name).node
    }

    override fun resolve(): PsiElement? {
        val qualified = siblings(forward = false)
                .filter { it.elementType == DTOLanguage.rule[DTOParser.RULE_qualifiedNamePart] }
                .map(PsiElement::getText)
                .toList()
                .asReversed()

        // 属性配置
        val config = parentOfType<DTOPropConfig>()
        if (config != null) {
            return config.resolveConfigParam(qualified)
        }

        // 类型使用
        val parent = parent
        if (parent is DTOQualifiedName) {
            if (parent.parts.size == 1) {
                return file.imported[part] ?: file.importedAlias[part]?.first
            }
        }

        // 全限定结构
        val psiFacade = JavaPsiFacade.getInstance(project)
        val clazz = psiFacade.findClass(qualified.joinToString("."), ProjectScope.getAllScope(project))
        return clazz ?: psiFacade.findPackage(qualified.joinToString("."))
    }

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitQualifiedNamePart(this)
        } else {
            super.accept(visitor)
        }
    }

    private fun DTOPropConfig.resolveConfigParam(qualified: List<String>): PsiElement? {
        if (name.text == PropConfigName.FetchType.text) {
            val scope = ProjectScope.getAllScope(project)
            val fetchTypeQualified = "org.babyfish.jimmer.sql.fetcher.ReferenceFetchType"
            val fetchType = JavaPsiFacade.getInstance(project).findClass(fetchTypeQualified, scope) ?: return null
            return fetchType.findFieldByName(qualified.first(), false)
        }

        val prop = parent as DTOPositiveProp
        val propPath = prop.propPath()
        val property = file.clazz.property(propPath + qualified)
        return property.source
    }
}

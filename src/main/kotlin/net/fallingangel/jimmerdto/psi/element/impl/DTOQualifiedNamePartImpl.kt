package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.lang.java.JavaLanguage
import com.intellij.psi.*
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.util.elementType
import com.intellij.psi.util.parentOfType
import com.intellij.psi.util.siblings
import net.fallingangel.jimmerdto.DTOLanguage
import net.fallingangel.jimmerdto.enums.PropConfigName
import net.fallingangel.jimmerdto.lsi.findPropertyOrNull
import net.fallingangel.jimmerdto.psi.DTOParser
import net.fallingangel.jimmerdto.psi.element.*
import net.fallingangel.jimmerdto.psi.mixin.impl.DTONamedElementImpl
import net.fallingangel.jimmerdto.util.*
import org.jetbrains.kotlin.idea.KotlinLanguage

class DTOQualifiedNamePartImpl(node: ASTNode) : DTONamedElementImpl(node), DTOQualifiedNamePart {
    override val part: String
        get() = nameIdentifier?.text ?: ""

    override fun getNameIdentifier(): PsiElement? {
        return findChildNullable("/qualifiedNamePart/Identifier")
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
        val psiFacade = JavaPsiFacade.getInstance(project)
        val scope = ProjectScope.getAllScope(project)
        if (parent is DTOQualifiedName && parent.parent !is DTOImportStatement) {
            if (parent.parts.size == 1) {
                // 类型定义和使用
                val imported = file.imported[part] ?: file.importedAlias[part]?.first
                if (imported == null && part in DTOLanguage.preludes) {
                    return when (file.projectLanguage) {
                        JavaLanguage.INSTANCE -> {
                            when (part) {
                                "Int" -> project.psiClass("java.lang.Integer")
                                "Char" -> project.psiClass("java.lang.Character")
                                else -> project.psiClass("java.lang.$part") ?: run {
                                    if (part.startsWith("Mutable")) {
                                        project.psiClass("java.util.${part.substring(7)}")
                                    } else {
                                        project.psiClass("java.util.$part")
                                    }
                                }
                            }
                        }

                        KotlinLanguage.INSTANCE -> {
                            val prelude = project.ktClass("kotlin.$part")
                                    .filter { "org.jetbrains.kotlin/kotlin-stdlib" in it.virtualFile.path }
                                    .getOrNull(0)
                            prelude ?: run {
                                project.ktClass("kotlin.collections.$part")
                                        .filter { "org.jetbrains.kotlin/kotlin-stdlib" in it.virtualFile.path }
                                        .getOrNull(0)
                            }
                        }

                        else -> null
                    }
                }
                return imported
            } else if (parent.parts.size == 2 && parent.parent !is DTOTypeRef) {
                // 枚举字面量
                return if (this == parent.parts[0]) {
                    file.imported[part] ?: file.importedAlias[part]?.second
                } else {
                    val enum = parent.parts[0].resolve() as? PsiClass ?: return null
                    enum.fields
                            .filterIsInstance<PsiEnumConstant>()
                            .find { it.name == part }
                }
            }
        }

        // 全限定结构
        val clazz = psiFacade.findClass(qualified.joinToString("."), scope)
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
        val scope = ProjectScope.getAllScope(project)
        val firstPart = qualified.first()
        val prop = parent as DTOPositiveProp
        val propPath = prop.propPath()

        return if (qualified.size == 1) {
            when (name.text) {
                PropConfigName.FetchType.text -> {
                    val fetchTypeQualified = "org.babyfish.jimmer.sql.fetcher.ReferenceFetchType"
                    val fetchType = JavaPsiFacade.getInstance(project).findClass(fetchTypeQualified, scope) ?: return null
                    fetchType.findFieldByName(firstPart, false)
                }

                PropConfigName.Filter.text, PropConfigName.Recursion.text -> {
                    file.imported[firstPart] ?: file.importedAlias[firstPart]?.second ?: JavaPsiFacade.getInstance(project).findPackage(firstPart)
                }

                else -> file.clazz.findPropertyOrNull(propPath + firstPart)?.source
            }
        } else {
            val resolvedProperty = file.clazz.findPropertyOrNull(propPath + qualified)?.source
            val resolvedPackage = resolvedProperty ?: JavaPsiFacade.getInstance(project).findPackage(qualified.joinToString("."))
            resolvedPackage ?: JavaPsiFacade.getInstance(project).findClass(qualified.joinToString("."), scope)
        }
    }
}

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
import net.fallingangel.jimmerdto.lsi.findProperty
import net.fallingangel.jimmerdto.lsi.jimmer.resolvedLClass
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
            ?: findChildNullable("/qualifiedNamePart/'like'")
            ?: findChildNullable("/qualifiedNamePart/'null'")
            ?: findChildNullable("/qualifiedNamePart/'desc'")
            ?: findChildNullable("/qualifiedNamePart/'asc'")
    }

    override fun newNameNode(name: String): ASTNode {
        return project.createQualifiedNamePart(name).node
    }

    override fun resolve(): PsiElement? {
        val morphism = parentOfType<DTOMorphism>()
        if (morphism != null && parentOfType<DTOQualifiedName>() == morphism.targetType && morphism.targetType?.parts?.size == 1) {
            return morphism.resolvedLClass?.source
        }

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
                val imported = file.importIndex[part]?.get(0)?.let { project.psiClass(it) }
                if (imported == null && part in DTOLanguage.preludes) {
                    return when (file.projectLanguage) {
                        JavaLanguage.INSTANCE -> {
                            when (part) {
                                "Int" -> project.psiClass("java.lang.Integer")
                                "Char" -> project.psiClass("java.lang.Character")
                                "Any" -> project.psiClass("java.lang.Object")
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
                            val prelude = arrayOf(
                                "kotlin",
                                "kotlin.annotation",
                                "kotlin.collections",
                                "kotlin.comparisons",
                                "kotlin.io",
                                "kotlin.ranges",
                                "kotlin.sequences",
                                "kotlin.text",
                                "kotlin.jvm",
                            )

                            prelude.firstNotNullOfOrNull { `package` ->
                                project.ktClass("$`package`.$part")
                                    // 过滤掉不是来自 kotlin-stdlib 的同名类
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
                    file.importIndex[part]?.get(0)?.let { project.psiClass(it) }
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
        val propClass = prop.property?.actualType?.resolvedLClass ?: return null

        return if (qualified.size == 1) {
            when (name.text) {
                PropConfigName.FetchType.text -> {
                    val fetchTypeQualified = "org.babyfish.jimmer.sql.fetcher.ReferenceFetchType"
                    val fetchType = JavaPsiFacade.getInstance(project).findClass(fetchTypeQualified, scope) ?: return null
                    fetchType.findFieldByName(firstPart, false)
                }

                PropConfigName.Filter.text, PropConfigName.Recursion.text -> {
                    file.importIndex[part]?.get(0)?.let { project.psiClass(it) }
                        ?: JavaPsiFacade.getInstance(project).findPackage(firstPart)
                }

                else -> propClass.findProperty(firstPart)?.source
                    ?: propClass.findProperty(firstPart.removeSuffix("Id"))?.source
            }
        } else {
            val resolvedProperty = propClass.findProperty(qualified)?.source
                ?: run {
                    val last = qualified.last()
                    propClass.findProperty(qualified.dropLast(1) + last.removeSuffix("Id"))?.source
                }
            val resolvedPackage = resolvedProperty ?: JavaPsiFacade.getInstance(project).findPackage(qualified.joinToString("."))
            resolvedPackage ?: JavaPsiFacade.getInstance(project).findClass(qualified.joinToString("."), scope)
        }
    }
}

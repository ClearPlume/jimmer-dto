package net.fallingangel.jimmerdto.psi.element

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.ProjectScope
import net.fallingangel.jimmerdto.enums.PropConfigName
import net.fallingangel.jimmerdto.lsi.LClass
import net.fallingangel.jimmerdto.lsi.LanguageProcessor
import net.fallingangel.jimmerdto.psi.mixin.DTOElement
import net.fallingangel.jimmerdto.psi.resolve.Resolution
import net.fallingangel.jimmerdto.util.*
import org.jetbrains.kotlin.psi.KtClass

interface DTOQualifiedName : DTOElement {
    val parts: List<DTOQualifiedNamePart>

    val value: String
        get() = parts.joinToString(".", transform = DTOQualifiedNamePart::part)

    val `package`: String
        get() = parts.dropLast(1).joinToString(".", transform = DTOQualifiedNamePart::part)

    val simpleName: String
        get() = parts.last().part

    val clazz: PsiClass?
        get() {
            val resolved = parts.last().resolve()
            // 只有在[类型使用]情景下，会解析到别名
            return if (resolved is DTOAlias) {
                when (val parent = resolved.parent) {
                    is DTOImportedType -> parent.type.resolve() as? PsiClass
                    is DTOImportStatement -> parent.qualifiedName.clazz
                    else -> null
                }
            } else {
                if (resolved is KtClass) {
                    val java = resolved.javaFqName ?: return null
                    JavaPsiFacade.getInstance(project).findClass(java, ProjectScope.getAllScope(project))
                } else {
                    resolved as? PsiClass
                }
            }
        }

    val initialSpace: Resolution.Space?
        get() {
            val config = parent<DTOPropConfig> { true }
            if (config != null) {
                // TODO propConfig 按名字拆 token。
                //  PropConfigName 是笼统的 '!' Identifier，导致 propConfig 的三个 LParen 分支靠顺序消歧，!orderBy(firstName)（无 asc/desc）被第一分支 LParen qualifiedName RParen 吞掉，PSI 结构与语义不符。
                //  改为每个配置名一个 lexer token，propConfig 按名字分派产生式。同时保留 PropConfigName 作为兜底，annotator 只要看到 unknownConfig，就直接“Unknown prop config name”。
                //  propConfig 节点提供自己的初始解析空间，DTOQualifiedName.initialSpace 只做转发。
                return when (config.name.text) {
                    PropConfigName.Where.text, PropConfigName.OrderBy.text -> config.containingLClass?.let(Resolution.Space::Properties)

                    // TODO 常量
                    PropConfigName.FetchType.text -> project.psiClass("org.babyfish.jimmer.sql.fetcher.ReferenceFetchType")
                        ?.let(Resolution.Space::Type)

                    else -> Resolution.Space.GlobalWithImports(file, file.entityPackage)
                }
            }

            return if (haveParent<DTOImportStatement>() || haveParent<DTOExportStatement>()) {
                Resolution.Space.GlobalRaw(file)
            } else {
                Resolution.Space.GlobalWithImports(file, file.entityPackage)
            }
        }

    val target: Resolution.Target?
        get() = parts.lastOrNull()?.target

    val resolvedLClass: LClass?
        get() {
            val source = target?.source ?: return null
            return LanguageProcessor.analyze(file).resolve(source) as? LClass
        }
}
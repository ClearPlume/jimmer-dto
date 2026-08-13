package net.fallingangel.jimmerdto.psi.element

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.ProjectScope
import net.fallingangel.jimmerdto.Constant
import net.fallingangel.jimmerdto.enums.PropConfigName
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
            val parent = parent
            if (parent is DTOMorphism) {
                val lClass = parent.containingLClass ?: return null
                return Resolution.Space.Subtypes(file, lClass)
            }

            val config = parent<DTOPropConfig>()
            if (config != null) {
                return when (config.name.text) {
                    PropConfigName.Where.text, PropConfigName.OrderBy.text -> config.containingLClass?.let(Resolution.Space::Properties)

                    PropConfigName.FetchType.text -> psiClass(Constant.REFERENCE_FETCH_TYPE)?.let(Resolution.Space::Type)

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
}
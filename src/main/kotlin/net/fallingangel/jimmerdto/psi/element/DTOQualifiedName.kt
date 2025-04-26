package net.fallingangel.jimmerdto.psi.element

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.ProjectScope
import net.fallingangel.jimmerdto.psi.mixin.DTOElement
import net.fallingangel.jimmerdto.util.javaFqName
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
}
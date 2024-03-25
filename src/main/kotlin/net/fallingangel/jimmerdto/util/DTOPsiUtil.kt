@file:JvmName("DTOPsiImplUtil")

package net.fallingangel.jimmerdto.util

import com.intellij.icons.AllIcons
import com.intellij.lang.jvm.JvmModifier
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import net.fallingangel.jimmerdto.psi.DTODto
import net.fallingangel.jimmerdto.psi.DTOFile
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.resolve.BindingContext
import javax.swing.Icon

object DTOPsiUtil {
    /**
     * 获取element元素所在dto文件中的所有DTO定义
     */
    fun findDTOs(element: PsiElement): List<DTODto> {
        val dtoFile = element.containingFile as DTOFile? ?: return emptyList()
        return PsiTreeUtil.getChildrenOfTypeAsList(dtoFile, DTODto::class.java)
    }
}

@Suppress("UnstableApiUsage")
val PsiClass.icon: Icon
    get() = when {
        isInterface -> AllIcons.Nodes.Interface
        isRecord -> AllIcons.Nodes.Record
        isAnnotationType -> AllIcons.Nodes.Annotationtype
        isEnum -> AllIcons.Nodes.Enum

        else -> if (hasModifier(JvmModifier.ABSTRACT)) {
            AllIcons.Nodes.AbstractClass
        } else {
            AllIcons.Nodes.Class
        }
    }

val KtClass.icon: Icon
    get() = when {
        isInterface() -> AllIcons.Nodes.Interface
        isAnnotation() -> AllIcons.Nodes.Annotationtype
        isEnum() -> AllIcons.Nodes.Enum

        else -> if (hasModifier(KtTokens.ABSTRACT_KEYWORD)) {
            AllIcons.Nodes.AbstractClass
        } else {
            AllIcons.Nodes.Class
        }
    }

val KtClass.qualifiedName: String
    get() = containingKtFile.packageFqName.asString()

fun KtAnnotated.hasAnnotation(annotation: String): Boolean {
    return annotationEntries.any {
        val context = it.analyze()
        context[BindingContext.ANNOTATION, it]?.fqName?.asString() == annotation
    }
}

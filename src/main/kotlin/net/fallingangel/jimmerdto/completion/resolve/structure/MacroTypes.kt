package net.fallingangel.jimmerdto.completion.resolve.structure

import com.intellij.lang.java.JavaLanguage
import com.intellij.psi.PsiClass
import com.intellij.psi.util.PsiTreeUtil
import net.fallingangel.jimmerdto.psi.DTOMacro
import net.fallingangel.jimmerdto.util.DTOPsiUtil
import net.fallingangel.jimmerdto.util.isSuperEntity
import net.fallingangel.jimmerdto.util.supers
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.KtClass

class MacroTypes : Structure<DTOMacro, List<String>> {
    /**
     * @param element Dto宏的参数元素
     *
     * @return 宏的可用参数类型列表
     */
    override fun value(element: DTOMacro): List<String> {
        val project = element.project
        val propClass = DTOPsiUtil.resolveMacroThis(element) ?: return emptyList()
        val supers = when (propClass.language) {
            JavaLanguage.INSTANCE -> {
                (propClass as PsiClass).supers()
                        .filter(PsiClass::isSuperEntity)
                        .mapNotNull { it.name }
                        .filter { it != "Object" }
            }

            KotlinLanguage.INSTANCE -> {
                val ktClass = PsiTreeUtil.findChildOfType(propClass.containingFile.virtualFile.toPsiFile(project), KtClass::class.java)
                (ktClass as KtClass).supers()
                        .filter(KtClass::isSuperEntity)
                        .mapNotNull(KtClass::getName)
            }

            else -> emptyList()
        }
        return supers + propClass.name!! + "this"
    }
}
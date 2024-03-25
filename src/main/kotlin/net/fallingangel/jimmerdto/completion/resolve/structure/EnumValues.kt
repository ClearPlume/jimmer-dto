package net.fallingangel.jimmerdto.completion.resolve.structure

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiEnumConstant
import net.fallingangel.jimmerdto.enums.Language
import net.fallingangel.jimmerdto.psi.DTOEnumInstance
import net.fallingangel.jimmerdto.psi.DTOPositiveProp
import net.fallingangel.jimmerdto.util.*
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtEnumEntry

class EnumValues : Structure<DTOEnumInstance, List<String>> {
    /**
     * @param element DTO或关联属性中的枚举属性元素
     *
     * @return 枚举可用实例
     */
    override fun value(element: DTOEnumInstance): List<String> {
        val project = element.project
        val enumProp = element.parent.parent.parent as DTOPositiveProp
        val classFile = element.virtualFile.entityFile(project) ?: return emptyList()

        return when (classFile.language) {
            Language.Java -> {
                classFile.psiClass(project, enumProp.propPath())?.enums ?: return emptyList()
            }

            Language.Kotlin -> {
                classFile.ktClass(project, enumProp.propPath())?.enums ?: return emptyList()
            }
        }
    }

    private val PsiClass.enums: List<String>
        get() = fields
                .filterIsInstance<PsiEnumConstant>()
                .map { it.name }

    private val KtClass.enums: List<String>
        get() = declarations
                .filterIsInstance<KtEnumEntry>()
                .map { it.name!! }
}
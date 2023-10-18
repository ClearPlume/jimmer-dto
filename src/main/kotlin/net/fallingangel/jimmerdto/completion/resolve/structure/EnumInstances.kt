package net.fallingangel.jimmerdto.completion.resolve.structure

import com.intellij.psi.PsiEnumConstant
import net.fallingangel.jimmerdto.psi.DTOEnumInstance
import net.fallingangel.jimmerdto.psi.DTOPositiveProp
import net.fallingangel.jimmerdto.util.*
import org.jetbrains.kotlin.psi.KtEnumEntry

class EnumInstances : Structure<DTOEnumInstance, List<String>> {
    /**
     * @param element Dto中的枚举属性元素
     *
     * @return 可用枚举实例
     */
    override fun value(element: DTOEnumInstance): List<String> {
        val project = element.project
        val prop = element.parent.parent.parent as DTOPositiveProp
        val classFile = element.virtualFile.entityFile(project) ?: return emptyList()

        return if (classFile.isJavaOrKotlin) {
            val psiClass = classFile.psiClass(project, prop.propPath()) ?: return emptyList()
            psiClass.fields
                    .filterIsInstance<PsiEnumConstant>()
                    .map { it.name }
        } else {
            val ktClass = classFile.ktClass(project, prop.propPath()) ?: return emptyList()
            ktClass.declarations
                    .filterIsInstance<KtEnumEntry>()
                    .map { it.name!! }
        }
    }
}
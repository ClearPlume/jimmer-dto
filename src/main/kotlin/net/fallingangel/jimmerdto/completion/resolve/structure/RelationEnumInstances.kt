package net.fallingangel.jimmerdto.completion.resolve.structure

import com.intellij.psi.PsiEnumConstant
import net.fallingangel.jimmerdto.psi.DTOEnumBody
import net.fallingangel.jimmerdto.psi.DTOPositiveProp
import net.fallingangel.jimmerdto.util.*
import org.jetbrains.kotlin.nj2k.postProcessing.type
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtProperty

class RelationEnumInstances : Structure<DTOEnumBody, List<String>> {
    /**
     * @param element 关联属性中的枚举属性元素
     *
     * @return 可用枚举实例
     */
    override fun value(element: DTOEnumBody): List<String> {
        val project = element.project
        val enumProp = element.parent as DTOPositiveProp
        val enumPropName = enumProp.propName.text

        val parentProp = enumProp.parent.parent.parent.parent as DTOPositiveProp
        val parentPropName = parentProp.propName.text
        val classFile = element.virtualFile.entityFile(project) ?: return emptyList()

        return if (classFile.isJavaOrKotlin) {
            val parentPropPsiClass = classFile.psiClass(project, parentPropName) ?: return emptyList()
            parentPropPsiClass.methods
                    .find { it.name == enumPropName }!!
                    .returnType!!
                    .clazz()!!
                    .fields
                    .filterIsInstance<PsiEnumConstant>()
                    .map { it.name }
        } else {
            val parentPropKtClass = classFile.ktClass(project, parentPropName) ?: return emptyList()
            parentPropKtClass.declarations
                    .filterIsInstance<KtProperty>()
                    .find { it.name == enumPropName }!!
                    .type()!!
                    .clazz()!!
                    .declarations
                    .filterIsInstance<KtEnumEntry>()
                    .map { it.name!! }
        }
    }
}
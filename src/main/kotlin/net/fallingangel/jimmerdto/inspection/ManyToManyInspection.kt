package net.fallingangel.jimmerdto.inspection

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiMethod
import net.fallingangel.jimmerdto.util.inspection

class ManyToManyInspection : AbstractBaseJavaLocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitMethod(method: PsiMethod) {
                super.visitMethod(method)

                val type = method.returnType as? PsiClassType ?: return
                val targetClass = if (type.hasParameters()) {
                    (type.parameters[0] as? PsiClassType)?.resolve()
                } else {
                    type.resolve()
                }
                targetClass ?: return

                val manyToOne = method.getAnnotation("org.babyfish.jimmer.sql.ManyToMany") ?: return
                val mappedBy = manyToOne.parameterList.attributes.find { it.name == "mappedBy" } ?: return

                if (mappedBy.literalValue !in targetClass.methods.map { it.name }) {
                    holder.registerProblem(
                        mappedBy.value?.originalElement!!,
                        inspection("inspection.dto.prop.display.name"),
                    )
                }
            }
        }
    }
}
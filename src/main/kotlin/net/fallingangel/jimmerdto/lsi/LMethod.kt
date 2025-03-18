package net.fallingangel.jimmerdto.lsi

import com.intellij.psi.PsiElement
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotation
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotationOwner
import net.fallingangel.jimmerdto.lsi.annotation.annotationsToString
import net.fallingangel.jimmerdto.lsi.param.LParam
import net.fallingangel.jimmerdto.lsi.param.LParamOwner
import net.fallingangel.jimmerdto.lsi.param.paramsToString

data class LMethod<M : PsiElement>(
    override val name: String,
    override val annotations: List<LAnnotation<*>>,
    override val params: List<LParam<*>>,
    val returnType: LReturnType,
    override val source: M,
) : LElement, LAnnotationOwner, LParamOwner, LPsiDependent {
    data class LReturnType(
        override val type: LType,
        override val annotations: List<LAnnotation<*>>,
        val methodAnnotations: List<LAnnotation<*>>,
    ) : LNullableAware {
        override val nullable: Boolean
            get() = methodAnnotations.any { it.name in listOf("Null", "Nullable") } || super.nullable

        fun toDebugString(visited: MutableSet<String>): String {
            val annotationsStr = annotationsToString(visited)
            return "LReturnType(type: ${type.toDebugString(visited)}, annotations=$annotationsStr)"
        }
    }

    override fun collectPsiElements(result: MutableSet<PsiElement>, visited: MutableSet<LPsiDependent>) {
        if (!visited.add(this)) {
            return
        }
        result.add(source)
        annotations.forEach { it.collectPsiElements(result, visited) }
        params.forEach { it.collectPsiElements(result, visited) }
        returnType.annotations.forEach { it.collectPsiElements(result, visited) }
        if (returnType.type is LClass<*>) {
            returnType.type.collectPsiElements(result, visited)
        } else if (returnType.type is LType.EnumType<*, *>) {
            returnType.type.collectPsiElements(result, visited)
        }
    }

    fun toDebugString(visited: MutableSet<String>): String {
        val paramsStr = paramsToString(visited)
        val annotationsStr = annotationsToString(visited)
        val returnTypeStr = returnType.toDebugString(visited)
        return "LMethod(name=$name, returnType=$returnTypeStr, params=$paramsStr, annotations=$annotationsStr)"
    }
}
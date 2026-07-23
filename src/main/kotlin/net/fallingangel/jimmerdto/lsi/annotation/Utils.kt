package net.fallingangel.jimmerdto.lsi.annotation

import com.intellij.psi.*
import kotlin.reflect.KClass
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotation.Param.Type as ParamType
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotation.Param.Value as ParamValue

fun LAnnotationOwner.hasAnnotation(annotationClass: KClass<out Annotation>): Boolean {
    return annotations.any { it.canonicalName == annotationClass.qualifiedName }
}

fun LAnnotationOwner.hasAnnotation(vararg annotationClass: KClass<out Annotation>): Boolean {
    return annotations.any { it.canonicalName in annotationClass.map(KClass<*>::qualifiedName) }
}

fun LAnnotationOwner.hasAnnotationBySimple(vararg simpleAnnotation: String): Boolean {
    return annotations.any { it.name in simpleAnnotation }
}

fun LAnnotationOwner.annotationsToString(visited: MutableSet<String>): String {
    return annotations.joinToString(prefix = "[", postfix = "]") { it.toDebugString(visited) }
}

fun resolveAnnotation(annotation: PsiAnnotation): LAnnotation? {
    val clazz = annotation.resolveAnnotationType() ?: return null
    val methods = clazz.methods.filterIsInstance<PsiAnnotationMethod>()
    val className = clazz.name ?: return null
    val canonicalName = clazz.qualifiedName ?: return null

    val values = annotation.parameterList.attributes
        // TODO Unresolved
        .mapNotNull { attribute ->
            val name = attribute.name ?: "value"
            val value = attribute.value ?: return@mapNotNull null
            name to resolveParamValue(value)
        }
        .toMap()

    val params = methods.mapNotNull { method ->
        val name = method.name
        val returnType = method.returnType ?: return@mapNotNull null
        val defaultValue = method.defaultValue

        LAnnotation.Param(
            name,
            // TODO Unresolved
            resolveParamType(returnType) ?: return@mapNotNull null,
            values[name],
            defaultValue?.let { resolveParamValue(it) },
            method,
        )
    }

    return LAnnotation(
        className,
        canonicalName,
        params,
        clazz,
    )
}

fun resolveParamType(type: PsiType): ParamType? {
    return when (type) {
        is PsiPrimitiveType -> ParamType.Scalar(
            when (type) {
                PsiTypes.booleanType() -> ParamType.Scalar.Kind.BOOLEAN
                PsiTypes.byteType() -> ParamType.Scalar.Kind.BYTE
                PsiTypes.shortType() -> ParamType.Scalar.Kind.SHORT
                PsiTypes.intType() -> ParamType.Scalar.Kind.INT
                PsiTypes.longType() -> ParamType.Scalar.Kind.LONG
                PsiTypes.floatType() -> ParamType.Scalar.Kind.FLOAT
                PsiTypes.doubleType() -> ParamType.Scalar.Kind.DOUBLE
                PsiTypes.charType() -> ParamType.Scalar.Kind.CHAR
                else -> return null
            }
        )

        is PsiArrayType -> ParamType.Array(resolveParamType(type.componentType) ?: return null)

        is PsiClassType -> {
            val typeClass = type.resolve() ?: return null
            when {
                typeClass.isEnum -> {
                    ParamType.Enum(
                        type.canonicalText,
                        typeClass.fields
                            .filterIsInstance<PsiEnumConstant>()
                            .map { it.name to it },
                        typeClass,
                    )
                }

                typeClass.isAnnotationType -> ParamType.Annotation(type.canonicalText, typeClass)

                type.canonicalText == "java.lang.String" -> ParamType.Scalar(ParamType.Scalar.Kind.STRING)

                typeClass.qualifiedName == "java.lang.Class" -> {
                    val typeParameters = type.parameters
                    val type0 = typeParameters.getOrNull(0) ?: return ParamType.Clazz(null, typeClass)

                    val boundClass = when (type0) {
                        is PsiWildcardType -> (type0.bound as? PsiClassType)?.resolve()
                        is PsiClassType -> type0.resolve()
                        else -> null
                    }

                    ParamType.Clazz(boundClass?.qualifiedName, boundClass ?: typeClass)
                }

                else -> null
            }
        }

        else -> null
    }
}

fun resolveParamValue(value: PsiAnnotationMemberValue): ParamValue? {
    return when (value) {
        is PsiLiteralExpression -> {
            ParamValue.Scalar(value.value ?: return null)
        }

        is PsiReferenceExpression -> {
            val enum = value.resolve() as? PsiEnumConstant ?: return null
            val className = enum.containingClass?.qualifiedName ?: return null
            ParamValue.Enum(className, enum.name)
        }

        is PsiClassObjectAccessExpression -> {
            ParamValue.Clazz(value.operand.type.canonicalText)
        }

        is PsiAnnotation -> {
            ParamValue.Annotation(resolveAnnotation(value) ?: return null)
        }
        // TODO Unresolved
        is PsiArrayInitializerMemberValue -> {
            ParamValue.Array(value.initializers.mapNotNull { resolveParamValue(it) })
        }

        is PsiPrefixExpression -> {
            val operand = value.operand as? PsiLiteralExpression ?: return null
            val value = when (val v = operand.value) {
                is Byte -> -v
                is Short -> -v
                is Int -> -v
                is Long -> -v
                is Float -> -v
                is Double -> -v
                else -> null
            }
            ParamValue.Scalar(value ?: return null)
        }

        else -> {
            null
        }
    }
}

infix fun ParamValue?.eq(constant: Enum<*>): Boolean {
    if (this !is ParamValue.Enum) return false
    val javaClass = constant.declaringJavaClass
    return canonicalName == javaClass.canonicalName && constantName == constant.name
}

infix fun ParamValue?.eq(expected: String): Boolean {
    return this is ParamValue.Scalar && value == expected
}

infix fun ParamValue?.eq(expected: Boolean): Boolean {
    return this is ParamValue.Scalar && value == expected
}
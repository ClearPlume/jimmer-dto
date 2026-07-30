package net.fallingangel.jimmerdto.lsi.processor

import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.*
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import net.fallingangel.jimmerdto.lsi.*
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotation
import net.fallingangel.jimmerdto.util.hasAnnotation
import net.fallingangel.jimmerdto.util.nullable
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.sql.Embeddable
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.MappedSuperclass
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotation.Param.Type as ParamType
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotation.Param.Value as ParamValue

class JavaProcessor : LanguageProcessor {
    private val childrenKey = Key.create<CachedValue<List<PsiClass>>>("JAVA_CACHED_CHILDREN_CLASS")

    override fun supports(language: Language): Boolean {
        return language == JavaLanguage.INSTANCE
    }

    context(element: PsiElement, types: ResolvedTypes)
    override fun lClass(): LClass? {
        val clazz = element.narrow<PsiClass>()
        val qualifiedName = clazz.qualifiedName ?: return null
        val name = clazz.name ?: return null

        return types.getOrPut(qualifiedName) {
            lateinit var lClass: LClass
            lClass = LClass(
                name,
                qualifiedName,
                // TODO Unresolved
                lazy { clazz.annotations.mapNotNull { resolveAnnotation(it) } },
                lazy { parents(clazz) },
                {
                    val classes = CachedValuesManager.getCachedValue(clazz, childrenKey) {
                        CachedValueProvider.Result.create(
                            children(clazz),
                            PsiModificationTracker.MODIFICATION_COUNT,
                        )
                    }
                    classes.mapNotNull { lClass(element = it, types = ResolvedTypes(qualifiedName to lClass)) }
                },
                lazy {
                    clazz.methods
                        .filter { !it.isConstructor }
                        // TODO Unresolved
                        .mapNotNull { lProperty(lClass, element = it) }
                },
                clazz,
            )
            lClass
        }
    }

    context(element: PsiElement, types: ResolvedTypes)
    override fun lProperty(containingLClass: LClass): LProperty? {
        val method = element.narrow<PsiMethod>()
        // TODO Unresolved
        val annotations = method.annotations.mapNotNull { resolveAnnotation(it) }
        // TODO 属性类型为Boolean时，jimmer.keepIsPrefix
        val methodName = method.name
        val name = if (methodName.startsWith("get") && methodName.length > 3 && methodName[3].isUpperCase()) {
            methodName[3].lowercaseChar() + methodName.substring(4)
        } else {
            methodName
        }
        val type = method.returnType ?: return null

        return LProperty(
            name,
            resolve(type) ?: return null,
            method.hasModifierProperty(PsiModifier.ABSTRACT),
            annotations,
            method,
            containingLClass,
        )
    }

    context(element: PsiElement)
    override fun isAnnotationClass(): Boolean {
        val clazz = element.narrow<PsiClass>()
        return clazz.isAnnotationType
    }

    context(project: Project)
    override fun builtinType(name: String): PsiElement? {
        TODO("Not yet implemented")
    }

    context(element: PsiElement)
    override fun canonicalName(): String? {
        val clazz = element.narrow<PsiClass>()
        return clazz.qualifiedName
    }

    context(element: PsiElement)
    override fun enum(): Pair<String, String>? {
        val enum = element.narrow<PsiEnumConstant>()
        val clazz = enum.containingClass ?: error("Enum constant ${enum.name} without containing class in ${enum.containingFile.name}")
        val canonicalName = clazz.qualifiedName ?: return null
        val entryName = enum.name
        return canonicalName to entryName
    }

    context(element: PsiElement)
    override fun lAnnotationParams(values: Map<String, ParamValue?>): List<LAnnotation.Param> {
        val clazz = element.narrow<PsiClass>()
        val methods = clazz.methods.filterIsInstance<PsiAnnotationMethod>()

        return methods.mapNotNull { method ->
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
    }

    context(types: ResolvedTypes)
    fun parents(clazz: PsiClass): List<LClass> {
        return clazz.supers
            .filter { it.qualifiedName != "java.lang.Object" }
            .mapNotNull { lClass(element = it) }
    }

    fun children(clazz: PsiClass): List<PsiClass> {
        return ClassInheritorsSearch.search(clazz, ProjectScope.getAllScope(clazz.project), false)
            .mapNotNull { it }
    }

    context(types: ResolvedTypes)
    fun resolve(type: PsiType): LProperty.Type? {
        return when (type) {
            is PsiPrimitiveType -> LProperty.Type.Scalar(type.name, type.nullable)

            is PsiArrayType -> {
                val componentType = type.componentType
                LProperty.Type.Array(resolve(componentType) ?: return null, componentType.nullable)
            }

            is PsiClassType -> {
                val typeClass = type.resolve() ?: return null
                when {
                    typeClass.isEnum -> {
                        LProperty.Type.Enum(
                            type.canonicalText,
                            typeClass.fields
                                .filterIsInstance<PsiEnumConstant>()
                                .map { it.name to it },
                            false,
                            typeClass,
                        )
                    }

                    type.hasParameters() -> {
                        val typeParameters = type.parameters
                        val rawType = type.rawType()
                        val type0 = typeParameters[0]
                        when (rawType.canonicalText) {
                            "java.util.List" -> LProperty.Type.Collection(
                                resolve(type0) ?: return null,
                                LProperty.Type.Collection.Kind.List,
                                type0.nullable,
                            )

                            "java.util.Queue" -> LProperty.Type.Collection(
                                resolve(type0) ?: return null,
                                LProperty.Type.Collection.Kind.Queue,
                                type0.nullable,
                            )

                            "java.util.Set" -> LProperty.Type.Collection(
                                resolve(type0) ?: return null,
                                LProperty.Type.Collection.Kind.Set,
                                type0.nullable,
                            )

                            "java.util.Map" -> LProperty.Type.Map(
                                resolve(type0) ?: return null,
                                resolve(typeParameters[1]) ?: return null,
                                false,
                            )

                            else -> LProperty.Type.Scalar(rawType.canonicalText, type.nullable)
                        }
                    }

                    else -> {
                        if (typeClass.hasAnnotation(Entity::class, MappedSuperclass::class, Embeddable::class, Immutable::class)) {
                            LProperty.Type.Clazz(lClass(element = typeClass) ?: return null, type.nullable, typeClass)
                        } else {
                            LProperty.Type.Scalar(type.canonicalText, type.nullable)
                        }
                    }
                }
            }

            else -> null
        }
    }

    fun resolveAnnotation(annotation: PsiAnnotation): LAnnotation? {
        val clazz = annotation.resolveAnnotationType() ?: return null
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

        val params = process(clazz) { lAnnotationParams(values) } ?: return null

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
}
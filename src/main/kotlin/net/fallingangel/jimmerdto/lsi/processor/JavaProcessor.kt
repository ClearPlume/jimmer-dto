package net.fallingangel.jimmerdto.lsi.processor

import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.util.Key
import com.intellij.psi.*
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import net.fallingangel.jimmerdto.lsi.LClass
import net.fallingangel.jimmerdto.lsi.LProperty
import net.fallingangel.jimmerdto.lsi.LanguageProcessor
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotation
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotationOwner
import net.fallingangel.jimmerdto.psi.DTOFile
import net.fallingangel.jimmerdto.util.hasAnnotation
import net.fallingangel.jimmerdto.util.nullable
import net.fallingangel.jimmerdto.util.psiClass
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.sql.Embeddable
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.MappedSuperclass
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotation.Param.Type as ParamType
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotation.Param.Value as ParamValue

class JavaProcessor : LanguageProcessor {
    private val childrenKey = Key.create<CachedValue<List<PsiClass>>>("JAVA_CACHED_CHILDREN_CLASS")

    override fun supports(dtoFile: DTOFile) = dtoFile.projectLanguage == JavaLanguage.INSTANCE

    override fun clazz(dtoFile: DTOFile): LClass? {
        val psiClass = dtoFile.project.psiClass(dtoFile.qualifiedEntity) ?: return null
        return clazz(psiClass, mutableMapOf())
    }

    fun clazz(clazz: PsiClass, resolvedType: MutableMap<String, LClass>): LClass? {
        val qualifiedName = clazz.qualifiedName ?: return null
        val name = clazz.name ?: return null

        return resolvedType.getOrPut(qualifiedName) {
            lateinit var lClass: LClass
            lClass = LClass(
                name,
                qualifiedName,
                // TODO Unresolved
                lazy { clazz.annotations.mapNotNull { resolve(it) } },
                lazy { parents(clazz, resolvedType) },
                {
                    val classes = CachedValuesManager.getCachedValue(clazz, childrenKey) {
                        CachedValueProvider.Result.create(
                            children(clazz),
                            PsiModificationTracker.MODIFICATION_COUNT,
                        )
                    }
                    val resolvedType = mutableMapOf(qualifiedName to lClass)
                    classes.mapNotNull { clazz(it, resolvedType) }
                },
                lazy { properties(clazz, lClass, resolvedType) },
                clazz,
            )
            lClass
        }
    }

    fun parents(clazz: PsiClass, resolvedType: MutableMap<String, LClass>): List<LClass> {
        return clazz.supers
            .filter { it.qualifiedName != "java.lang.Object" }
            .mapNotNull { clazz(it, resolvedType) }
    }

    fun children(clazz: PsiClass): List<PsiClass> {
        return ClassInheritorsSearch.search(clazz, ProjectScope.getAllScope(clazz.project), false)
            .mapNotNull { it }
    }

    fun properties(clazz: PsiClass, containingLClass: LClass, resolvedType: MutableMap<String, LClass>): List<LProperty> {
        return clazz.methods
            .filter { !it.isConstructor }
            // TODO Unresolved
            .mapNotNull { resolve(it, containingLClass, resolvedType) }
    }

    override fun resolve(element: PsiElement): LAnnotationOwner? {
        return when (element) {
            is PsiClass -> clazz(element, mutableMapOf())
            is PsiMethod -> {
                val owner = clazz(element.containingClass ?: return null, mutableMapOf())
                owner?.allProperties?.firstOrNull { it.name == element.name }
            }

            else -> null
        }
    }

    fun resolve(method: PsiMethod, containingLClass: LClass, resolvedType: MutableMap<String, LClass>): LProperty? {
        // TODO Unresolved
        val annotations = method.annotations.mapNotNull { resolve(it) }
        // TODO 属性类型为Boolean时，jimmer.keepIsPrefix
        val methodName = method.name
        val name = if (methodName.startsWith("get") && methodName.length > 3 && methodName[3].isUpperCase()) {
            methodName[3].lowercaseChar() + methodName.substring(4)
        } else {
            methodName
        }

        return LProperty(
            name,
            resolve(method.returnType ?: return null, resolvedType) ?: return null,
            method.hasModifierProperty(PsiModifier.ABSTRACT),
            annotations,
            method,
            containingLClass,
        )
    }

    fun resolve(type: PsiType, resolvedType: MutableMap<String, LClass>): LProperty.Type? {
        return when (type) {
            is PsiPrimitiveType -> LProperty.Type.Scalar(type.name, type.nullable)

            is PsiArrayType -> {
                val componentType = type.componentType
                LProperty.Type.Array(resolve(componentType, resolvedType) ?: return null, componentType.nullable)
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
                                resolve(type0, resolvedType) ?: return null,
                                LProperty.Type.Collection.Kind.List,
                                type0.nullable,
                            )

                            "java.util.Queue" -> LProperty.Type.Collection(
                                resolve(type0, resolvedType) ?: return null,
                                LProperty.Type.Collection.Kind.Queue,
                                type0.nullable,
                            )

                            "java.util.Set" -> LProperty.Type.Collection(
                                resolve(type0, resolvedType) ?: return null,
                                LProperty.Type.Collection.Kind.Set,
                                type0.nullable,
                            )

                            "java.util.Map" -> LProperty.Type.Map(
                                resolve(type0, resolvedType) ?: return null,
                                resolve(typeParameters[1], resolvedType) ?: return null,
                                false,
                            )

                            else -> LProperty.Type.Scalar(rawType.canonicalText, type.nullable)
                        }
                    }

                    else -> {
                        if (typeClass.hasAnnotation(Entity::class, MappedSuperclass::class, Embeddable::class, Immutable::class)) {
                            LProperty.Type.Clazz(clazz(typeClass, resolvedType) ?: return null, type.nullable, typeClass)
                        } else {
                            LProperty.Type.Scalar(type.canonicalText, type.nullable)
                        }
                    }
                }
            }

            else -> null
        }
    }

    fun resolve(annotation: PsiAnnotation): LAnnotation? {
        val clazz = annotation.resolveAnnotationType() ?: return null
        val methods = clazz.methods.filterIsInstance<PsiAnnotationMethod>()
        val className = clazz.name ?: return null
        val canonicalName = clazz.qualifiedName ?: return null

        val values = annotation.parameterList.attributes
            // TODO Unresolved
            .mapNotNull { attribute ->
                val value = attribute.value ?: return@mapNotNull null
                attribute.name to resolveParamValue(value)
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
                ParamValue.Annotation(resolve(value) ?: return null)
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
}
package net.fallingangel.jimmerdto.lsi.processor

import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
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
import net.fallingangel.jimmerdto.util.ktClass
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.sql.Embeddable
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.MappedSuperclass
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotation.Param.Type as ParamType
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotation.Param.Value as ParamValue

class KotlinProcessor : LanguageProcessor {
    private val childrenKey = Key.create<CachedValue<List<KtClass>>>("KOTLIN_CACHED_CHILDREN_CLASS")

    override fun supports(dtoFile: DTOFile) = dtoFile.projectLanguage == KotlinLanguage.INSTANCE

    override fun clazz(dtoFile: DTOFile): LClass? {
        val ktClass = dtoFile.project.ktClass(dtoFile.qualifiedEntity).getOrNull(0) ?: return null
        return clazz(ktClass, mutableMapOf())
    }

    fun clazz(clazz: KtClass, resolvedType: MutableMap<String, LClass>): LClass? {
        val name = clazz.name ?: return null
        val qualifiedName = clazz.fqName?.asString() ?: return null

        return resolvedType.getOrPut(qualifiedName) {
            lateinit var lClass: LClass
            lClass = LClass(
                name,
                qualifiedName,
                lazy {
                    analyze(clazz) {
                        // TODO Unresolved
                        clazz.symbol.annotations.mapNotNull { resolve(it) }
                    }
                },
                lazy { parents(clazz, resolvedType) },
                {
                    val classes = CachedValuesManager.getCachedValue(clazz, childrenKey) {
                        CachedValueProvider.Result.create(
                            children(clazz),
                            PsiModificationTracker.MODIFICATION_COUNT,
                            DumbService.getInstance(clazz.project).modificationTracker,
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

    fun parents(clazz: KtClass, resolvedType: MutableMap<String, LClass>): List<LClass> {
        return analyze(clazz) {
            val symbol = clazz.symbol as? KaClassSymbol ?: return emptyList()
            symbol.superTypes
                .filter { it.symbol!!.hasAnnotation(MappedSuperclass::class, Entity::class) }
                .mapNotNull { it.symbol?.psi as? KtClass }
                .mapNotNull { clazz(it, resolvedType) }
        }
    }

    fun children(clazz: KtClass): List<KtClass> {
        val lightClass = clazz.toLightClass() ?: return emptyList()
        return ClassInheritorsSearch.search(lightClass, ProjectScope.getAllScope(clazz.project), false)
            .mapNotNull { (it as? KtLightClass)?.kotlinOrigin as? KtClass }
    }

    fun properties(clazz: KtClass, containingLClass: LClass, resolvedType: MutableMap<String, LClass>): List<LProperty> {
        // TODO Unresolved
        return clazz.getProperties().mapNotNull { resolve(it, containingLClass, resolvedType) }
    }

    override fun resolve(element: PsiElement): LAnnotationOwner? {
        return when (element) {
            is KtClass -> clazz(element, mutableMapOf())
            is KtProperty -> {
                val owner = clazz(element.containingClass()!!, mutableMapOf())
                owner?.allProperties?.firstOrNull { it.name == element.name }
            }

            is KtLightClass -> {
                val ktClass = element.kotlinOrigin as? KtClass ?: return null
                clazz(ktClass, mutableMapOf())
            }

            else -> null
        }
    }

    fun resolve(property: KtProperty, containingLClass: LClass, resolvedType: MutableMap<String, LClass>): LProperty? {
        return analyze(property) {
            // TODO Unresolved
            val annotations = property.symbol.annotations.mapNotNull { resolve(it) }
            val type = resolve(property.symbol.returnType, resolvedType) ?: return null
            LProperty(property.name ?: return null, type, property.isAbstract(), annotations, property, containingLClass)
        }
    }

    fun KaSession.resolve(type: KaType, resolvedType: MutableMap<String, LClass>): LProperty.Type? {
        if (type !is KaClassType) {
            return null
        }

        val nullable = type.isMarkedNullable
        val symbol = type.symbol
        val classId = symbol.classId ?: return null
        val fqName = classId.asFqNameString()

        return when {
            fqName == "kotlin.IntArray" -> LProperty.Type.Array(LProperty.Type.Scalar("kotlin.Int", false), nullable)
            fqName == "kotlin.LongArray" -> LProperty.Type.Array(LProperty.Type.Scalar("kotlin.Long", false), nullable)
            fqName == "kotlin.BooleanArray" -> LProperty.Type.Array(LProperty.Type.Scalar("kotlin.Boolean", false), nullable)
            fqName == "kotlin.ByteArray" -> LProperty.Type.Array(LProperty.Type.Scalar("kotlin.Byte", false), nullable)
            fqName == "kotlin.ShortArray" -> LProperty.Type.Array(LProperty.Type.Scalar("kotlin.Short", false), nullable)
            fqName == "kotlin.CharArray" -> LProperty.Type.Array(LProperty.Type.Scalar("kotlin.Char", false), nullable)
            fqName == "kotlin.FloatArray" -> LProperty.Type.Array(LProperty.Type.Scalar("kotlin.Float", false), nullable)
            fqName == "kotlin.DoubleArray" -> LProperty.Type.Array(LProperty.Type.Scalar("kotlin.Double", false), nullable)

            fqName == "kotlin.Array" -> when (val typeArgument = type.typeArguments[0]) {
                is KaStarTypeProjection -> null
                is KaTypeArgumentWithVariance -> LProperty.Type.Array(resolve(typeArgument.type, resolvedType) ?: return null, nullable)
            }

            (symbol as? KaClassSymbol)?.classKind == KaClassKind.ENUM_CLASS -> {
                LProperty.Type.Enum(
                    fqName,
                    symbol.staticDeclaredMemberScope
                        .declarations
                        .filterIsInstance<KaEnumEntrySymbol>()
                        .mapNotNull { it.name.asString() to (it.psi ?: return@mapNotNull null) }
                        .toList(),
                    nullable,
                    symbol.psi,
                )
            }

            fqName == "kotlin.collections.List" || fqName == "java.util.List" -> {
                val argType = type.typeArguments.first()
                LProperty.Type.Collection(
                    resolve(argType, resolvedType) ?: return null,
                    LProperty.Type.Collection.Kind.List,
                    nullable,
                )
            }

            fqName == "kotlin.collections.Set" || fqName == "java.util.Set" -> {
                val argType = type.typeArguments.first()
                LProperty.Type.Collection(
                    resolve(argType, resolvedType) ?: return null,
                    LProperty.Type.Collection.Kind.Set,
                    nullable,
                )
            }

            fqName == "kotlin.collections.Map" || fqName == "java.util.Map" -> {
                val keyType = type.typeArguments[0]
                val valueType = type.typeArguments[1]
                LProperty.Type.Map(
                    resolve(keyType, resolvedType) ?: return null,
                    resolve(valueType, resolvedType) ?: return null,
                    nullable,
                )
            }

            else -> {
                val psi = symbol.psi as? KtClass ?: return LProperty.Type.Scalar(fqName, nullable)
                if (symbol.hasAnnotation(Entity::class, MappedSuperclass::class, Embeddable::class, Immutable::class)) {
                    LProperty.Type.Clazz(clazz(psi, resolvedType) ?: return null, nullable, psi)
                } else {
                    LProperty.Type.Scalar(fqName, nullable)
                }
            }
        }
    }

    fun KaSession.resolve(type: KaTypeProjection, resolvedType: MutableMap<String, LClass>): LProperty.Type? {
        return when (type) {
            is KaStarTypeProjection -> null
            is KaTypeArgumentWithVariance -> resolve(type.type, resolvedType)
        }
    }

    fun KaSession.resolve(annotation: KaAnnotation): LAnnotation? {
        val constructor = annotation.constructorSymbol ?: return null
        val classId = annotation.classId ?: return null
        val values = annotation.arguments
            // TODO Unresolved
            .mapNotNull { argument ->
                val name = argument.name.asString()
                val value = resolveParamValue(argument.expression) ?: return@mapNotNull null
                name to value
            }
            .toMap()

        return LAnnotation(
            classId.shortClassName.asString(),
            classId.asFqNameString(),
            // TODO Unresolved
            constructor.valueParameters.mapNotNull { parameter ->
                val name = parameter.name.asString()
                val type = resolveParamType(parameter.returnType) ?: return@mapNotNull null

                LAnnotation.Param(
                    name,
                    type,
                    values[name],
                    null,
                    parameter.psi,
                )
            },
            annotation.psi,
        )
    }

    fun KaSession.resolveParamType(type: KaType): ParamType? {
        val type = type as? KaClassType ?: return null

        return when (type.classId.asFqNameString()) {
            "kotlin.IntArray" -> ParamType.Array(ParamType.Scalar(ParamType.Scalar.Kind.INT))
            "kotlin.LongArray" -> ParamType.Array(ParamType.Scalar(ParamType.Scalar.Kind.LONG))
            "kotlin.BooleanArray" -> ParamType.Array(ParamType.Scalar(ParamType.Scalar.Kind.BOOLEAN))
            "kotlin.ByteArray" -> ParamType.Array(ParamType.Scalar(ParamType.Scalar.Kind.BYTE))
            "kotlin.ShortArray" -> ParamType.Array(ParamType.Scalar(ParamType.Scalar.Kind.SHORT))
            "kotlin.CharArray" -> ParamType.Array(ParamType.Scalar(ParamType.Scalar.Kind.CHAR))
            "kotlin.FloatArray" -> ParamType.Array(ParamType.Scalar(ParamType.Scalar.Kind.FLOAT))
            "kotlin.DoubleArray" -> ParamType.Array(ParamType.Scalar(ParamType.Scalar.Kind.DOUBLE))

            "kotlin.Array" -> when (val typeArgument = type.typeArguments[0]) {
                is KaStarTypeProjection -> null
                is KaTypeArgumentWithVariance -> ParamType.Array(resolveParamType(typeArgument.type) ?: return null)
            }

            "kotlin.reflect.KClass" -> when (val typeArgument = type.typeArguments[0]) {
                is KaStarTypeProjection -> ParamType.Clazz(null, type.symbol.psi)
                is KaTypeArgumentWithVariance -> ParamType.Clazz((typeArgument.type as? KaClassType)?.classId?.asFqNameString(), type.symbol.psi)
            }

            else -> {
                val fqName = type.classId.asFqNameString()

                when (val symbol = type.symbol) {
                    is KaNamedClassSymbol if symbol.classKind == KaClassKind.ENUM_CLASS -> {
                        ParamType.Enum(
                            fqName,
                            symbol.staticDeclaredMemberScope
                                .declarations
                                .filterIsInstance<KaEnumEntrySymbol>()
                                .mapNotNull { it.name.asString() to (it.psi ?: return@mapNotNull null) }
                                .toList(),
                            symbol.psi,
                        )
                    }

                    is KaNamedClassSymbol if symbol.classKind == KaClassKind.ANNOTATION_CLASS -> ParamType.Annotation(fqName, symbol.psi)
                    else -> ParamType.Scalar(scalarKind(fqName) ?: return null)
                }
            }
        }
    }

    private fun scalarKind(fqName: String): ParamType.Scalar.Kind? = when (fqName) {
        "kotlin.Boolean" -> ParamType.Scalar.Kind.BOOLEAN
        "kotlin.Byte" -> ParamType.Scalar.Kind.BYTE
        "kotlin.Short" -> ParamType.Scalar.Kind.SHORT
        "kotlin.Int" -> ParamType.Scalar.Kind.INT
        "kotlin.Long" -> ParamType.Scalar.Kind.LONG
        "kotlin.Float" -> ParamType.Scalar.Kind.FLOAT
        "kotlin.Double" -> ParamType.Scalar.Kind.DOUBLE
        "kotlin.Char" -> ParamType.Scalar.Kind.CHAR
        "kotlin.String" -> ParamType.Scalar.Kind.STRING
        else -> null
    }

    fun KaSession.resolveParamValue(expression: KaAnnotationValue): ParamValue? {
        return when (expression) {
            // TODO Unresolved
            is KaAnnotationValue.ArrayValue -> ParamValue.Array(expression.values.mapNotNull { resolveParamValue(it) })

            is KaAnnotationValue.ClassLiteralValue -> ParamValue.Clazz((expression.classId ?: return null).asFqNameString())

            is KaAnnotationValue.ConstantValue -> ParamValue.Scalar(expression.value.value ?: return null)

            is KaAnnotationValue.EnumEntryValue -> {
                val callableId = expression.callableId ?: return null
                val packageName = callableId.packageName.asString()
                val className = callableId.className?.asString() ?: return null
                ParamValue.Enum("$packageName.$className", callableId.callableName.asString())
            }

            is KaAnnotationValue.NestedAnnotationValue -> {
                val annotation = resolve(expression.annotation) ?: return null
                ParamValue.Annotation(annotation)
            }

            is KaAnnotationValue.UnsupportedValue -> null
        }
    }
}
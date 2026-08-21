package net.fallingangel.jimmerdto.lsi.processor

import com.intellij.lang.Language
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import net.fallingangel.jimmerdto.enums.StandardType
import net.fallingangel.jimmerdto.lsi.*
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotation
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotationSite
import net.fallingangel.jimmerdto.lsi.jimmer.JimmerAnnotations
import net.fallingangel.jimmerdto.lsi.jimmer.JimmerTypes
import net.fallingangel.jimmerdto.psi.demand
import net.fallingangel.jimmerdto.util.hasAnnotation
import net.fallingangel.jimmerdto.util.ktClass
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaAnnotatedSymbol
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.analysis.utils.classId
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotation.Param.Type as ParamType
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotation.Param.Value as ParamValue

class KotlinProcessor : LanguageProcessor, CompilerContext {
    private val childrenKey = Key.create<CachedValue<List<KtClass>>>("KOTLIN_CACHED_CHILDREN_CLASS")

    private val KtClassOrObject.lName: LName
        get() = demand(KtClassOrObject::getClassId).lName

    private val ClassId.lName: LName
        get() {
            return LName(
                packageFqName.asString(),
                relativeClassName.pathSegments().dropLast(1).map(Name::asString),
                shortClassName.asString(),
            )
        }

    override fun supports(language: Language): Boolean {
        return language == KotlinLanguage.INSTANCE
    }

    override fun appliesTo(module: Module): Boolean {
        return KotlinFacet.get(module) != null
    }

    context(element: PsiElement, types: ResolvedTypes)
    override fun lClass(): LClass {
        val clazz = element.narrow<KtClass>()
        val name = clazz.lName
        val qualifiedName = name.fqName

        return types.getOrPut(qualifiedName) {
            lateinit var lClass: LClass
            lClass = LClass(
                name,
                lazy {
                    analyze(clazz) {
                        // TODO Unresolved
                        clazz.symbol.annotations.mapNotNull { resolve(it) }
                    }
                },
                lazy { parents(clazz) },
                {
                    val classes = CachedValuesManager.getCachedValue(clazz, childrenKey) {
                        CachedValueProvider.Result.create(
                            children(clazz),
                            PsiModificationTracker.MODIFICATION_COUNT,
                            DumbService.getInstance(clazz.project).modificationTracker,
                        )
                    }
                    classes.mapNotNull { lClass(element = it, types = ResolvedTypes(qualifiedName to lClass)) }
                },
                lazy { clazz.getProperties().mapNotNull { lProperty(lClass, element = it) } },
                clazz,
            )
            lClass
        }
    }

    context(element: PsiElement, types: ResolvedTypes)
    override fun lProperty(containingLClass: LClass): LProperty? {
        val property = element.narrow<KtProperty>()
        return analyze(property) {
            // TODO Unresolved
            val annotations = property.symbol.annotations.mapNotNull { resolve(it) }
            val type = resolve(property.symbol.returnType) ?: return null
            LProperty(property.name ?: return null, type, property.isAbstract(), annotations, property, containingLClass)
        }
    }

    context(element: PsiElement)
    override fun containingClass(): PsiNamedElement? {
        val property = element as? KtProperty ?: return null
        return property.containingClass()
    }

    context(element: PsiElement)
    override fun isAnnotationClass(): Boolean {
        val clazz = element.narrow<KtClass>()
        return clazz.isAnnotation()
    }

    context(element: PsiElement)
    override fun isEnumClass(): Boolean {
        val clazz = element.narrow<KtClass>()
        return clazz.isEnum()
    }

    context(element: PsiElement)
    override fun builtinType(type: StandardType): PsiNamedElement? {
        val qualified = when (type) {
            StandardType.Boolean -> "kotlin.Boolean"
            StandardType.Char -> "kotlin.Char"
            StandardType.Byte -> "kotlin.Byte"
            StandardType.Short -> "kotlin.Short"
            StandardType.Int -> "kotlin.Int"
            StandardType.Long -> "kotlin.Long"
            StandardType.Float -> "kotlin.Float"
            StandardType.Double -> "kotlin.Double"
            StandardType.Any -> "kotlin.Any"
            StandardType.String -> "kotlin.String"
            StandardType.Array -> "kotlin.Array"
            StandardType.Iterable -> "kotlin.collections.Iterable"
            StandardType.MutableIterable -> "kotlin.collections.MutableIterable"
            StandardType.Collection -> "kotlin.collections.Collection"
            StandardType.MutableCollection -> "kotlin.collections.MutableCollection"
            StandardType.List -> "kotlin.collections.List"
            StandardType.MutableList -> "kotlin.collections.MutableList"
            StandardType.Set -> "kotlin.collections.Set"
            StandardType.MutableSet -> "kotlin.collections.MutableSet"
            StandardType.Map -> "kotlin.collections.Map"
            StandardType.MutableMap -> "kotlin.collections.MutableMap"
        }
        return LName.fromFqn(qualified).ktClass()
    }

    context(element: PsiElement)
    override fun className(): LName {
        val clazz = element.narrow<KtClassOrObject>()
        return clazz.lName
    }

    context(element: PsiElement)
    override fun qualifiedEnumConstant(): Pair<LName, String>? {
        val enum = element.narrow<KtEnumEntry>()
        val clazz = enum.containingClass() ?: error("Enum constant ${enum.name} without containing class in ${enum.containingFile.name}")
        val entryName = enum.name ?: return null
        return clazz.lName to entryName
    }

    @OptIn(KaExperimentalApi::class)
    context(element: PsiElement)
    override fun lAnnotationParams(values: Map<String, ParamValue?>): List<LAnnotation.Param> {
        val clazz = element.narrow<KtClass>()
        return analyze(clazz) {
            val valueParameters = clazz.primaryConstructor?.symbol?.valueParameters ?: return emptyList()
            // TODO Unresolved
            valueParameters.mapNotNull { parameter ->
                val name = parameter.name.asString()
                val type = resolveParamType(parameter) ?: return@mapNotNull null
                val source = parameter.psi as? KtParameter ?: return@mapNotNull null

                val defaultValue = source.defaultValue?.let { defaultValue ->
                    val defaultValue = defaultValue.evaluateAsAnnotationValue() ?: return@let null
                    resolveParamValue(defaultValue)
                }

                LAnnotation.Param(name, type, values[name], defaultValue, source)
            }
        }
    }

    context(element: PsiElement)
    override fun kind(): LKind? {
        return when (element) {
            is KtEnumEntry -> null

            is KtClass -> when {
                element.isAnnotation() -> LKind.Annotation
                element.isInterface() -> LKind.Interface
                element.isEnum() -> LKind.Enum
                else -> LKind.Class
            }

            is KtParameter -> {
                val containingClass = element.containingClass() ?: return null
                when {
                    containingClass.isAnnotation() -> LKind.Parameter
                    else -> null
                }
            }

            is KtProperty -> {
                val containingClass = element.containingClass() ?: return null
                when {
                    containingClass.isInterface() -> LKind.Property
                    else -> null
                }
            }

            else -> null
        }
    }

    context(element: PsiElement)
    override fun hasAnnotation(vararg annotation: LName): Boolean {
        val declaration = element.narrow<KtDeclaration>()
        val classIds = analyze(declaration) {
            val symbol = declaration.symbol as? KaAnnotatedSymbol ?: return false
            symbol.annotations.classIds
        }
        return annotation.map(LName::toClassId).any { it in classIds }
    }

    context(element: PsiElement)
    override fun typeArgumentFor(superName: LName, index: Int): PsiNamedElement? {
        val clazz = element.narrow<KtClass>()
        return analyze(clazz) {
            val symbol = clazz.symbol as? KaClassSymbol ?: return null
            val superType = symbol.defaultType.allSupertypes
                .filterIsInstance<KaClassType>()
                .firstOrNull { superName.toClassId() == it.classId }
                ?: return null
            val typeParameter = superType.typeArguments.getOrNull(index)?.type ?: return null
            (typeParameter as? KaClassType)?.symbol?.psi as? PsiNamedElement
        }
    }

    context(element: PsiElement)
    override fun topLevelClasses(): List<PsiNamedElement> {
        return (element as? KtFile)?.declarations?.filterIsInstance<KtClass>().orEmpty()
    }

    context(element: PsiElement)
    override fun builtinAliases(): List<String> {
        val qualifiedName = className().fqName
        return when (qualifiedName) {
            "kotlin.Boolean" -> listOf(StandardType.Boolean.name)
            "kotlin.Char" -> listOf(StandardType.Char.name)
            "kotlin.Byte" -> listOf(StandardType.Byte.name)
            "kotlin.Short" -> listOf(StandardType.Short.name)
            "kotlin.Int" -> listOf(StandardType.Int.name)
            "kotlin.Long" -> listOf(StandardType.Long.name)
            "kotlin.Float" -> listOf(StandardType.Float.name)
            "kotlin.Double" -> listOf(StandardType.Double.name)
            "kotlin.Any" -> listOf(StandardType.Any.name)
            "kotlin.String" -> listOf(StandardType.String.name)
            "kotlin.Array" -> listOf(StandardType.Array.name)
            "kotlin.collections.Iterable" -> listOf(StandardType.Iterable.name)
            "kotlin.collections.MutableIterable" -> listOf(StandardType.MutableIterable.name)
            "kotlin.collections.Collection" -> listOf(StandardType.Collection.name)
            "kotlin.collections.MutableCollection" -> listOf(StandardType.MutableCollection.name)
            "kotlin.collections.List" -> listOf(StandardType.List.name)
            "kotlin.collections.MutableList" -> listOf(StandardType.MutableList.name)
            "kotlin.collections.Set" -> listOf(StandardType.Set.name)
            "kotlin.collections.MutableSet" -> listOf(StandardType.MutableSet.name)
            "kotlin.collections.Map" -> listOf(StandardType.Map.name)
            "kotlin.collections.MutableMap" -> listOf(StandardType.MutableMap.name)
            else -> emptyList()
        }
    }

    context(element: PsiElement)
    override fun nestedTypes(): List<PsiNamedElement> {
        val clazz = element.narrow<KtClass>()
        return clazz.body?.declarations?.filterIsInstance<KtClassOrObject>()?.filter { it !is KtEnumEntry }.orEmpty()
    }

    context(element: PsiElement)
    override fun enumConstants(): List<PsiNamedElement> {
        val clazz = element.narrow<KtClass>()
        return clazz.body?.declarations?.filterIsInstance<KtEnumEntry>().orEmpty()
    }

    context(_: PsiElement)
    override fun filterEntity(filterClass: PsiElement): PsiNamedElement? {
        return process(filterClass) { typeArgumentFor(JimmerTypes.KFieldFilter) }
    }

    context(_: PsiElement)
    override fun fieldFilterName(): LName {
        return JimmerTypes.KFieldFilter
    }

    context(element: PsiElement)
    override fun isInheritorOrSelf(base: LName): Boolean? {
        val clazz = element.narrow<KtClassOrObject>()

        return analyze(clazz) {
            val classSymbol = clazz.classSymbol ?: return null
            classSymbol.defaultType.isSubtypeOf(base.toClassId())
        }
    }

    context(element: PsiElement)
    override fun isInheritorOrSelf(base: PsiElement): Boolean? {
        val clazz = element.narrow<KtClassOrObject>()

        return analyze(clazz) {
            val classSymbol = clazz.classSymbol ?: return null
            val baseId = when (base) {
                is KtClassOrObject -> base.getClassId()
                is PsiClass -> base.classId
                else -> error("Unexpected base: ${base::class}")
            } ?: return null
            classSymbol.defaultType.isSubtypeOf(baseId)
        }
    }

    context(element: PsiElement)
    override fun annotationSites(): Set<LAnnotationSite> {
        val clazz = element.narrow<KtClass>()
        val propSites = arrayOf("FIELD", "PROPERTY_GETTER", "FUNCTION", "PROPERTY_SETTER", "PROPERTY")

        val sites = analyze(clazz) {
            val target = clazz.symbol.annotations[ClassId.fromString("kotlin/annotation/Target")].singleOrNull()
            target ?: return emptySet()
            val argument = target.arguments.singleOrNull() ?: return emptySet()

            val value = argument.expression as KaAnnotationValue.ArrayValue
            value.values
                .mapNotNullTo(mutableSetOf()) {
                    if (it !is KaAnnotationValue.EnumEntryValue) return@mapNotNullTo null
                    val site = it.callableId?.callableName?.asString()
                    if (site in propSites) {
                        LAnnotationSite.Prop
                    } else {
                        null
                    }
                }
        }
        return sites
    }

    context(types: ResolvedTypes)
    fun parents(clazz: KtClass): List<LClass> {
        return analyze(clazz) {
            val symbol = clazz.symbol as? KaClassSymbol ?: return emptyList()
            symbol.superTypes
                .mapNotNull { it.symbol?.psi as? KtClass }
                .map { lClass(element = it) }
        }
    }

    fun children(clazz: KtClass): List<KtClass> {
        val lightClass = clazz.toLightClass() ?: return emptyList()
        return ClassInheritorsSearch.search(lightClass, ProjectScope.getAllScope(clazz.project), false)
            .mapNotNull { (it as? KtLightClass)?.kotlinOrigin as? KtClass }
    }

    context(types: ResolvedTypes)
    fun KaSession.resolve(type: KaType): LProperty.Type? {
        if (type !is KaClassType) {
            return null
        }

        val nullable = type.isMarkedNullable
        val symbol = type.symbol
        val source = symbol.psi ?: return null
        val lName = symbol.classId?.lName ?: return null
        val fqName = lName.fqName

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
                is KaTypeArgumentWithVariance -> LProperty.Type.Array(resolve(typeArgument.type) ?: return null, nullable)
            }

            (symbol as? KaClassSymbol)?.classKind == KaClassKind.ENUM_CLASS -> {
                LProperty.Type.Enum(
                    lName,
                    symbol.staticDeclaredMemberScope
                        .declarations
                        .filterIsInstance<KaEnumEntrySymbol>()
                        .mapNotNull { it.name.asString() to (it.psi ?: return@mapNotNull null) }
                        .toList(),
                    nullable,
                    source,
                )
            }

            fqName == "kotlin.collections.List" || fqName == "java.util.List" -> {
                val argType = type.typeArguments.first()
                LProperty.Type.Collection(
                    resolve(argType) ?: return null,
                    LProperty.Type.Collection.Kind.List,
                    nullable,
                )
            }

            fqName == "kotlin.collections.Set" || fqName == "java.util.Set" -> {
                val argType = type.typeArguments.first()
                LProperty.Type.Collection(
                    resolve(argType) ?: return null,
                    LProperty.Type.Collection.Kind.Set,
                    nullable,
                )
            }

            fqName == "kotlin.collections.Map" || fqName == "java.util.Map" -> {
                val keyType = type.typeArguments[0]
                val valueType = type.typeArguments[1]
                LProperty.Type.Map(
                    resolve(keyType) ?: return null,
                    resolve(valueType) ?: return null,
                    nullable,
                )
            }

            else -> {
                if (
                    symbol.hasAnnotation(
                        JimmerAnnotations.Entity,
                        JimmerAnnotations.MappedSuperclass,
                        JimmerAnnotations.Embeddable,
                        JimmerAnnotations.Immutable
                    )
                ) {
                    LProperty.Type.Clazz(lClass(element = source), nullable, source)
                } else {
                    LProperty.Type.Scalar(fqName, nullable)
                }
            }
        }
    }

    context(types: ResolvedTypes)
    fun KaSession.resolve(type: KaTypeProjection): LProperty.Type? {
        return when (type) {
            is KaStarTypeProjection -> null
            is KaTypeArgumentWithVariance -> resolve(type.type)
        }
    }

    fun KaSession.resolve(annotation: KaAnnotation): LAnnotation? {
        val classId = annotation.classId ?: return null
        val clazz = findClass(classId)?.psi ?: return null
        val values = annotation.arguments
            // TODO Unresolved
            .mapNotNull { argument ->
                val name = argument.name.asString()
                val value = resolveParamValue(argument.expression) ?: return@mapNotNull null
                name to value
            }
            .toMap()

        val params = process(clazz) { lAnnotationParams(values) } ?: return null
        val sites = process(clazz) { annotationSites() } ?: return null

        return LAnnotation(
            classId.lName,
            params,
            sites + LAnnotationSite.Type,
            annotation.psi ?: return null,
        )
    }

    fun KaSession.resolveParamType(parameter: KaValueParameterSymbol): ParamType? {
        val type = resolveParamType(parameter.returnType) ?: return null
        return if (parameter.isVararg) {
            ParamType.Array(type)
        } else {
            type
        }
    }

    fun KaSession.resolveParamType(type: KaType): ParamType? {
        val type = type as? KaClassType ?: return null

        return when (type.classId.asFqNameString()) {
            "kotlin.IntArray" -> ParamType.Array(ParamType.Scalar(ParamType.Scalar.Kind.Int))
            "kotlin.LongArray" -> ParamType.Array(ParamType.Scalar(ParamType.Scalar.Kind.Long))
            "kotlin.BooleanArray" -> ParamType.Array(ParamType.Scalar(ParamType.Scalar.Kind.Boolean))
            "kotlin.ByteArray" -> ParamType.Array(ParamType.Scalar(ParamType.Scalar.Kind.Byte))
            "kotlin.ShortArray" -> ParamType.Array(ParamType.Scalar(ParamType.Scalar.Kind.Short))
            "kotlin.CharArray" -> ParamType.Array(ParamType.Scalar(ParamType.Scalar.Kind.Char))
            "kotlin.FloatArray" -> ParamType.Array(ParamType.Scalar(ParamType.Scalar.Kind.Float))
            "kotlin.DoubleArray" -> ParamType.Array(ParamType.Scalar(ParamType.Scalar.Kind.Double))

            "kotlin.Array" -> when (val typeArgument = type.typeArguments[0]) {
                is KaStarTypeProjection -> null
                is KaTypeArgumentWithVariance -> ParamType.Array(resolveParamType(typeArgument.type) ?: return null)
            }

            "kotlin.reflect.KClass" -> when (val typeArgument = type.typeArguments[0]) {
                is KaStarTypeProjection -> ParamType.Clazz(null, null)
                is KaTypeArgumentWithVariance -> {
                    val symbol = typeArgument.type.symbol
                    ParamType.Clazz(symbol?.classId?.lName, symbol?.psi)
                }
            }

            else -> {
                when (val symbol = type.symbol) {
                    is KaNamedClassSymbol if symbol.classKind == KaClassKind.ENUM_CLASS -> {
                        ParamType.Enum(
                            type.classId.lName,
                            type.symbol.psi ?: return null,
                            symbol.staticDeclaredMemberScope
                                .declarations
                                .filterIsInstance<KaEnumEntrySymbol>()
                                .mapNotNull { it.name.asString() to (it.psi ?: return@mapNotNull null) }
                                .toList(),
                        )
                    }

                    is KaNamedClassSymbol if symbol.classKind == KaClassKind.ANNOTATION_CLASS -> ParamType.Annotation(type.classId.lName)
                    else -> ParamType.Scalar(scalarKind(type.classId.lName.fqName) ?: return null)
                }
            }
        }
    }

    private fun scalarKind(fqName: String): ParamType.Scalar.Kind? = when (fqName) {
        "kotlin.Boolean" -> ParamType.Scalar.Kind.Boolean
        "kotlin.Byte" -> ParamType.Scalar.Kind.Byte
        "kotlin.Short" -> ParamType.Scalar.Kind.Short
        "kotlin.Int" -> ParamType.Scalar.Kind.Int
        "kotlin.Long" -> ParamType.Scalar.Kind.Long
        "kotlin.Float" -> ParamType.Scalar.Kind.Float
        "kotlin.Double" -> ParamType.Scalar.Kind.Double
        "kotlin.Char" -> ParamType.Scalar.Kind.Char
        "kotlin.String" -> ParamType.Scalar.Kind.String
        else -> null
    }

    fun KaSession.resolveParamValue(expression: KaAnnotationValue): ParamValue? {
        return when (expression) {
            // TODO Unresolved
            is KaAnnotationValue.ArrayValue -> ParamValue.Array(expression.values.mapNotNull { resolveParamValue(it) })

            is KaAnnotationValue.ClassLiteralValue -> {
                val classId = expression.classId ?: return null
                val clazz = expression.type.symbol?.psi ?: return null
                ParamValue.Clazz(classId.lName, clazz)
            }

            is KaAnnotationValue.ConstantValue -> ParamValue.Scalar(expression.value.value ?: return null)

            is KaAnnotationValue.EnumEntryValue -> {
                val callableId = expression.callableId ?: return null
                val classId = callableId.classId ?: return null
                ParamValue.Enum(classId.lName, callableId.callableName.asString())
            }

            is KaAnnotationValue.NestedAnnotationValue -> {
                val annotation = resolve(expression.annotation) ?: return null
                ParamValue.Annotation(annotation)
            }

            is KaAnnotationValue.UnsupportedValue -> null
        }
    }
}

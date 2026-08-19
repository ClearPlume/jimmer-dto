package net.fallingangel.jimmerdto.lsi.processor

import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.module.Module
import com.intellij.openapi.util.Key
import com.intellij.psi.*
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.util.*
import net.fallingangel.jimmerdto.enums.StandardType
import net.fallingangel.jimmerdto.lsi.*
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotation
import net.fallingangel.jimmerdto.lsi.jimmer.JimmerAnnotations
import net.fallingangel.jimmerdto.lsi.jimmer.JimmerOptions
import net.fallingangel.jimmerdto.psi.demand
import net.fallingangel.jimmerdto.util.hasAnnotation
import net.fallingangel.jimmerdto.util.nullable
import net.fallingangel.jimmerdto.util.psiClass
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.idea.base.util.module
import org.jetbrains.kotlin.psi.KtClassOrObject
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotation.Param.Type as ParamType
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotation.Param.Value as ParamValue

class JavaProcessor : LanguageProcessor, CompilerContext {
    private val childrenKey = Key.create<CachedValue<List<PsiClass>>>("JAVA_CACHED_CHILDREN_CLASS")

    private val PsiClass.lName: LName
        get() {
            val outer = generateSequence(this) { it.containingClass }.toList().asReversed()
            val pkg = (outer.first().containingFile as PsiJavaFile).packageName
            val nesting = outer.dropLast(1).map { it.demand(PsiClass::getName) }
            return LName(pkg, nesting, demand(PsiClass::getName))
        }

    override fun supports(language: Language): Boolean {
        return language == JavaLanguage.INSTANCE
    }

    override fun appliesTo(module: Module): Boolean {
        return true
    }

    context(element: PsiElement, types: ResolvedTypes)
    override fun lClass(): LClass {
        val clazz = element.narrow<PsiClass>()
        val lName = clazz.lName
        val qualifiedName = lName.fqName

        return types.getOrPut(qualifiedName) {
            lateinit var lClass: LClass
            lClass = LClass(
                lName,
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

        val type = method.returnType ?: return null
        val keepIsPrefix = JimmerOptions.of(element.module).keepIsPrefix
        val methodName = method.name
        val name = when {
            !keepIsPrefix && type == PsiTypes.booleanType() && methodName.startsWith("is") && methodName.length > 2 && methodName[2].isUpperCase() -> {
                methodName[2].lowercase() + methodName.substring(3)
            }

            methodName.startsWith("get") && methodName.length > 3 && methodName[3].isUpperCase() -> {
                methodName[3].lowercase() + methodName.substring(4)
            }

            else -> methodName
        }

        // TODO Unresolved
        val annotations = method.annotations.mapNotNull { resolveAnnotation(it) }

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
    override fun containingClass(): PsiNamedElement? {
        val property = element as? PsiMethod ?: return null
        return property.containingClass
    }

    context(element: PsiElement)
    override fun isAnnotationClass(): Boolean {
        val clazz = element.narrow<PsiClass>()
        return clazz.isAnnotationType
    }

    context(element: PsiElement)
    override fun isEnumClass(): Boolean {
        val clazz = element.narrow<PsiClass>()
        return clazz.isEnum
    }

    context(element: PsiElement)
    override fun builtinType(type: StandardType): PsiNamedElement? {
        val qualified = when (type) {
            StandardType.Boolean -> "java.lang.Boolean"
            StandardType.Char -> "java.lang.Character"
            StandardType.Byte -> "java.lang.Byte"
            StandardType.Short -> "java.lang.Short"
            StandardType.Int -> "java.lang.Integer"
            StandardType.Long -> "java.lang.Long"
            StandardType.Float -> "java.lang.Float"
            StandardType.Double -> "java.lang.Double"
            StandardType.Any -> "java.lang.Object"
            StandardType.String -> "java.lang.String"
            StandardType.Array -> null
            StandardType.Iterable, StandardType.MutableIterable -> "java.lang.Iterable"
            StandardType.Collection, StandardType.MutableCollection -> "java.util.Collection"
            StandardType.List, StandardType.MutableList -> "java.util.List"
            StandardType.Set, StandardType.MutableSet -> "java.util.Set"
            StandardType.Map, StandardType.MutableMap -> "java.util.Map"
        }
        return qualified?.let(element::psiClass)
    }

    context(element: PsiElement)
    override fun className(): LName {
        val clazz = element.narrow<PsiClass>()
        return clazz.lName
    }

    context(element: PsiElement)
    override fun qualifiedEnumConstant(): Pair<LName, String> {
        val enum = element.narrow<PsiEnumConstant>()
        val clazz = enum.containingClass ?: error("Enum constant ${enum.name} without containing class in ${enum.containingFile.name}")
        val entryName = enum.name
        return clazz.lName to entryName
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

    context(element: PsiElement)
    override fun kind(): LKind? {
        return when (element) {
            is PsiTypeParameter -> null

            is PsiClass -> when {
                element.isAnnotationType -> LKind.Annotation
                element.isInterface -> LKind.Interface
                element.isEnum -> LKind.Enum
                else -> LKind.Class
            }

            is PsiAnnotationMethod -> LKind.Parameter

            is PsiMethod -> {
                val containingClass = element.containingClass ?: return null
                when {
                    containingClass.isInterface -> LKind.Property
                    else -> null
                }
            }

            else -> null
        }
    }

    context(element: PsiElement)
    override fun hasAnnotation(vararg annotation: LName): Boolean {
        val annotated = element.narrow<PsiModifierListOwner>()
        return annotation.map(LName::fqName).any(annotated::hasAnnotation)
    }

    context(element: PsiElement)
    override fun typeArgumentFor(superName: String, index: Int): PsiNamedElement? {
        val clazz = element.narrow<PsiClass>()
        val superClass = element.psiClass(superName) ?: return null
        val substitutor = TypeConversionUtil.getClassSubstitutor(superClass, clazz, PsiSubstitutor.EMPTY) ?: return null
        val typeParameter = superClass.typeParameters.getOrNull(index) ?: return null
        val substituted = substitutor.substitute(typeParameter) ?: return null
        return (substituted as? PsiClassType)?.resolve()
    }

    context(element: PsiElement)
    override fun topLevelClasses(): List<PsiNamedElement> {
        return (element as? PsiJavaFile)?.classes?.asList().orEmpty()
    }

    context(element: PsiElement)
    override fun builtinAliases(): List<String> {
        val qualifiedName = className().fqName
        return when (qualifiedName) {
            "java.lang.Boolean" -> listOf(StandardType.Boolean.name)
            "java.lang.Character" -> listOf(StandardType.Char.name)
            "java.lang.Byte" -> listOf(StandardType.Byte.name)
            "java.lang.Short" -> listOf(StandardType.Short.name)
            "java.lang.Integer" -> listOf(StandardType.Int.name)
            "java.lang.Long" -> listOf(StandardType.Long.name)
            "java.lang.Float" -> listOf(StandardType.Float.name)
            "java.lang.Double" -> listOf(StandardType.Double.name)
            "java.lang.Object" -> listOf(StandardType.Any.name)
            "java.lang.String" -> listOf(StandardType.String.name)
            "java.lang.Iterable" -> listOf(StandardType.Iterable.name, StandardType.MutableIterable.name)
            "java.util.Collection" -> listOf(StandardType.Collection.name, StandardType.MutableCollection.name)
            "java.util.List" -> listOf(StandardType.List.name, StandardType.MutableList.name)
            "java.util.Set" -> listOf(StandardType.Set.name, StandardType.MutableSet.name)
            "java.util.Map" -> listOf(StandardType.Map.name, StandardType.MutableMap.name)
            else -> emptyList()
        }
    }

    context(element: PsiElement)
    override fun nestedTypes(): List<PsiNamedElement> {
        val clazz = element.narrow<PsiClass>()
        return clazz.innerClasses.toList()
    }

    context(element: PsiElement)
    override fun enumConstants(): List<PsiNamedElement> {
        val clazz = element.narrow<PsiClass>()
        return clazz.fields.filterIsInstance<PsiEnumConstant>()
    }

    context(_: PsiElement)
    override fun filterEntity(filterClass: PsiElement): PsiNamedElement? {
        val tableClass = process(filterClass) { typeArgumentFor("org.babyfish.jimmer.sql.fetcher.FieldFilter") } ?: return null
        return process(tableClass) { typeArgumentFor("org.babyfish.jimmer.sql.ast.table.Table") }
    }

    context(_: PsiElement)
    override fun fieldFilterName(): LName {
        return LName("org.babyfish.jimmer.sql.fetcher", "FieldFilter")
    }

    context(element: PsiElement)
    override fun isInheritorOrSelf(base: String): Boolean? {
        val baseClass = element.psiClass(base) ?: return null
        return isInheritorOrSelf(baseClass)
    }

    context(element: PsiElement)
    override fun isInheritorOrSelf(base: PsiElement): Boolean? {
        val clazz = element.narrow<PsiClass>()
        val baseClass = when (base) {
            is PsiClass -> base
            is KtClassOrObject -> base.toLightClass()
            else -> error("Unexpected base: ${base::class}")
        } ?: return null
        return InheritanceUtil.isInheritorOrSelf(clazz, baseClass, true)
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
                            typeClass.lName,
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
                        if (
                            typeClass.hasAnnotation(
                                JimmerAnnotations.Entity,
                                JimmerAnnotations.MappedSuperclass,
                                JimmerAnnotations.Embeddable,
                                JimmerAnnotations.Immutable
                            )
                        ) {
                            LProperty.Type.Clazz(lClass(element = typeClass), type.nullable, typeClass)
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
            clazz.lName,
            params,
            clazz,
        )
    }

    fun resolveParamType(type: PsiType): ParamType? {
        return when (type) {
            is PsiPrimitiveType -> ParamType.Scalar(
                when (type) {
                    PsiTypes.booleanType() -> ParamType.Scalar.Kind.Boolean
                    PsiTypes.byteType() -> ParamType.Scalar.Kind.Byte
                    PsiTypes.shortType() -> ParamType.Scalar.Kind.Short
                    PsiTypes.intType() -> ParamType.Scalar.Kind.Int
                    PsiTypes.longType() -> ParamType.Scalar.Kind.Long
                    PsiTypes.floatType() -> ParamType.Scalar.Kind.Float
                    PsiTypes.doubleType() -> ParamType.Scalar.Kind.Double
                    PsiTypes.charType() -> ParamType.Scalar.Kind.Char
                    else -> return null
                }
            )

            is PsiArrayType -> ParamType.Array(resolveParamType(type.componentType) ?: return null)

            is PsiClassType -> {
                val typeClass = type.resolve() ?: return null
                when {
                    typeClass.isEnum -> {
                        ParamType.Enum(
                            typeClass.lName,
                            typeClass.fields
                                .filterIsInstance<PsiEnumConstant>()
                                .map { it.name to it },
                        )
                    }

                    typeClass.isAnnotationType -> ParamType.Annotation(typeClass.lName)

                    type.canonicalText == "java.lang.String" -> ParamType.Scalar(ParamType.Scalar.Kind.String)

                    typeClass.qualifiedName == "java.lang.Class" -> {
                        val typeParameters = type.parameters
                        val type0 = typeParameters.getOrNull(0) ?: return ParamType.Clazz(null, null)

                        val boundClass = when (type0) {
                            is PsiWildcardType -> (type0.bound as? PsiClassType)?.resolve()
                            is PsiClassType -> type0.resolve()
                            else -> null
                        }
                        ParamType.Clazz(boundClass?.lName, boundClass)
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
                val className = enum.containingClass?.lName ?: return null
                ParamValue.Enum(className, enum.name)
            }

            is PsiClassObjectAccessExpression -> {
                val clazz = when (val type = value.operand.type) {
                    is PsiClassType -> type.resolve()
                    is PsiPrimitiveType -> type.getBoxedType(value)?.resolve()
                    else -> null
                } ?: return null
                ParamValue.Clazz(clazz.lName, clazz)
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

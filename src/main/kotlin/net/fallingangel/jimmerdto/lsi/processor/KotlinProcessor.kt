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
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.base.utils.fqname.fqName
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.completion.reference
import org.jetbrains.kotlin.idea.refactoring.isAbstract
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.constants.*
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlinx.serialization.compiler.resolve.toClassDescriptor
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
                // TODO Unresolved
                lazy { clazz.annotationEntries.mapNotNull { resolve(it) } },
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
        return clazz.superTypeListEntries
            .mapNotNull { it.typeReference?.typeElement as? KtUserType }
            .mapNotNull { it.referenceExpression?.reference()?.resolve() as? KtClass }
            .filter { it.hasAnnotation(MappedSuperclass::class, Entity::class) }
            .mapNotNull { clazz(it, resolvedType) }
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
        // TODO Unresolved
        val annotations = property.annotationEntries.mapNotNull { resolve(it) }
        val returnType = (property.resolveToDescriptorIfAny() as? CallableDescriptor)?.returnType ?: return null
        val type = resolve(returnType, resolvedType) ?: return null
        return LProperty(property.name ?: return null, type, property.isAbstract(), annotations, property, containingLClass)
    }

    fun resolve(type: KotlinType, resolvedType: MutableMap<String, LClass>): LProperty.Type? {
        val nullable = type.isMarkedNullable
        val descriptor = type.toClassDescriptor ?: return null
        val fqName = descriptor.fqNameSafe.asString()

        return when {
            KotlinBuiltIns.isPrimitiveType(type) -> LProperty.Type.Scalar(fqName, nullable)

            KotlinBuiltIns.isArray(type) -> LProperty.Type.Array(
                resolve(DefaultBuiltIns.Instance.getArrayElementType(type), resolvedType) ?: return null,
                nullable,
            )

            else -> {
                when {
                    descriptor.kind.isEnumClass -> {
                        LProperty.Type.Enum(
                            fqName,
                            descriptor.unsubstitutedMemberScope
                                .getContributedDescriptors()
                                .filterIsInstance<ClassDescriptor>()
                                .filter { it.kind == ClassKind.ENUM_ENTRY }
                                .mapNotNull {
                                    val element = DescriptorToSourceUtils.getSourceFromDescriptor(it) ?: return@mapNotNull null
                                    it.name.asString() to element
                                },
                            nullable,
                            descriptor.source.getPsi(),
                        )
                    }

                    descriptor.kind.isClass || descriptor.kind.isInterface -> {
                        when (fqName) {
                            "kotlin.collections.List", "java.util.List" -> {
                                val argType = type.arguments.first().type
                                LProperty.Type.Collection(
                                    resolve(argType, resolvedType) ?: return null,
                                    LProperty.Type.Collection.Kind.List,
                                    nullable,
                                )
                            }

                            "kotlin.collections.Set", "java.util.Set" -> {
                                val argType = type.arguments.first().type
                                LProperty.Type.Collection(
                                    resolve(argType, resolvedType) ?: return null,
                                    LProperty.Type.Collection.Kind.Set,
                                    nullable,
                                )
                            }

                            "kotlin.collections.Map", "java.util.Map" -> {
                                val keyType = type.arguments[0].type
                                val valueType = type.arguments[1].type
                                LProperty.Type.Map(
                                    resolve(keyType, resolvedType) ?: return null,
                                    resolve(valueType, resolvedType) ?: return null,
                                    nullable,
                                )
                            }

                            else -> {
                                val ktClass = DescriptorToSourceUtils.getSourceFromDescriptor(descriptor) as? KtClass
                                if (
                                    ktClass != null && ktClass.hasAnnotation(
                                        Entity::class,
                                        MappedSuperclass::class,
                                        Embeddable::class,
                                        Immutable::class,
                                    )
                                ) {
                                    LProperty.Type.Clazz(clazz(ktClass, resolvedType) ?: return null, nullable, ktClass)
                                } else {
                                    LProperty.Type.Scalar(fqName, nullable)
                                }
                            }
                        }
                    }

                    else -> null
                }
            }
        }
    }

    fun resolve(annotation: KtAnnotationEntry): LAnnotation? {
        val annotation = annotation.analyze()[BindingContext.ANNOTATION, annotation] ?: return null
        return resolve(annotation)
    }

    fun resolve(annotation: AnnotationDescriptor): LAnnotation? {
        val qualifiedName = annotation.fqName?.asString() ?: return null
        val descriptor = annotation.annotationClass ?: return null

        val values = annotation.allValueArguments
            // TODO Unresolved
            .mapNotNull { it.key.asString() to resolveParamValue(it.value) }
            .toMap()

        val params = descriptor
            .unsubstitutedMemberScope
            .getContributedDescriptors()
            .filterIsInstance<PropertyDescriptor>()
            // TODO Unresolved
            .mapNotNull {
                val name = it.name.asString()
                val type = resolveParamType(it.type) ?: return@mapNotNull null

                LAnnotation.Param(
                    name,
                    type,
                    values[name],
                    null,
                    it.source.getPsi(),
                )
            }

        return LAnnotation(
            qualifiedName.substringAfterLast('.'),
            qualifiedName,
            params,
            annotation.source.getPsi(),
        )
    }

    fun resolveParamType(type: KotlinType): ParamType? {
        val primitiveType = KotlinBuiltIns.getPrimitiveType(type)
        if (primitiveType != null) {
            return ParamType.Scalar(
                when (primitiveType) {
                    PrimitiveType.BOOLEAN -> ParamType.Scalar.Kind.BOOLEAN
                    PrimitiveType.CHAR -> ParamType.Scalar.Kind.CHAR
                    PrimitiveType.BYTE -> ParamType.Scalar.Kind.BYTE
                    PrimitiveType.SHORT -> ParamType.Scalar.Kind.SHORT
                    PrimitiveType.INT -> ParamType.Scalar.Kind.INT
                    PrimitiveType.FLOAT -> ParamType.Scalar.Kind.FLOAT
                    PrimitiveType.LONG -> ParamType.Scalar.Kind.LONG
                    PrimitiveType.DOUBLE -> ParamType.Scalar.Kind.DOUBLE
                }
            )
        }

        if (KotlinBuiltIns.isString(type)) {
            return ParamType.Scalar(ParamType.Scalar.Kind.STRING)
        }

        if (KotlinBuiltIns.isArray(type) || KotlinBuiltIns.isPrimitiveArray(type)) {
            val elementType = resolveParamType(DefaultBuiltIns.Instance.getArrayElementType(type)) ?: return null
            return ParamType.Array(elementType)
        }

        val descriptor = type.toClassDescriptor ?: return null
        val fqName = descriptor.fqNameSafe.asString()
        val source = descriptor.source.getPsi()

        if (descriptor.kind.isEnumClass) {
            return ParamType.Enum(
                fqName,
                descriptor.unsubstitutedMemberScope
                    .getContributedDescriptors()
                    .filterIsInstance<ClassDescriptor>()
                    .filter { it.kind == ClassKind.ENUM_ENTRY }
                    .mapNotNull {
                        val element = DescriptorToSourceUtils.getSourceFromDescriptor(it) ?: return@mapNotNull null
                        it.name.asString() to element
                    },
                source,
            )
        }

        if (descriptor.kind.isAnnotationClass) {
            return ParamType.Annotation(fqName, source)
        }

        if (fqName == "kotlin.reflect.KClass") {
            val type0 = type.arguments.getOrNull(0) ?: return ParamType.Clazz(null, source)

            if (type0.isStarProjection) {
                return ParamType.Clazz(null, source)
            }

            return ParamType.Clazz(type0.type.fqName?.asString(), source)
        }

        return null
    }

    fun resolveParamValue(value: ConstantValue<*>): ParamValue? {
        return when (value) {
            // TODO Unresolved
            is ArrayValue -> ParamValue.Array(value.value.mapNotNull { resolveParamValue(it) })

            is KClassValue -> when (val value = value.value) {
                is KClassValue.Value.LocalClass -> null
                is KClassValue.Value.NormalClass -> ParamValue.Clazz(value.classId.asFqNameString())
            }

            is ByteValue, is ShortValue, is IntValue, is LongValue,
            is FloatValue, is DoubleValue, is BooleanValue, is CharValue,
            is StringValue -> ParamValue.Scalar(value.value)

            is EnumValue -> ParamValue.Enum(value.enumClassId.asFqNameString(), value.enumEntryName.asString())

            is AnnotationValue -> {
                val annotation = resolve(value.value) ?: return null
                ParamValue.Annotation(annotation)
            }

            else -> null
        }
    }
}
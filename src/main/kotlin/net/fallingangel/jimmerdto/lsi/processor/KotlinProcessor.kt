package net.fallingangel.jimmerdto.lsi.processor

import com.intellij.psi.PsiElement
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.search.searches.ClassInheritorsSearch
import net.fallingangel.jimmerdto.lsi.*
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotation
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotationOwner
import net.fallingangel.jimmerdto.lsi.param.LParam
import net.fallingangel.jimmerdto.psi.DTOFile
import net.fallingangel.jimmerdto.util.isInSource
import net.fallingangel.jimmerdto.util.ktClass
import net.fallingangel.jimmerdto.util.qualifiedName
import org.babyfish.jimmer.sql.MappedSuperclass
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlinx.serialization.compiler.resolve.toClassDescriptor

class KotlinProcessor : LanguageProcessor<KtClass> {
    override fun supports(dtoFile: DTOFile) = dtoFile.projectLanguage == KotlinLanguage.INSTANCE

    override fun clazz(dtoFile: DTOFile): LClass<KtClass> {
        val ktClass = dtoFile.project.ktClass(dtoFile.qualifiedEntity).getOrNull(0)
        ktClass ?: throw IllegalStateException("Entity class for $dtoFile not found")
        return clazz(ktClass, mutableMapOf())
    }

    fun clazz(clazz: KtClass, resolvedType: MutableMap<String, LClass<KtClass>>): LClass<KtClass> {
        val qualifiedName = clazz.fqName?.asString()!!
        return resolvedType.getOrPut(qualifiedName) {
            lateinit var lClass: LClass<KtClass>
            lClass = LClass(
                clazz.name!!,
                qualifiedName,
                false,
                clazz.isAnnotation(),
                clazz.annotationEntries.map { resolve(it, resolvedType) },
                lazy { parents(clazz, resolvedType) },
                lazy { children(clazz, resolvedType) },
                lazy { properties(clazz, lClass, resolvedType) },
                lazy { methods(clazz, resolvedType) },
                clazz,
            )
            lClass
        }
    }

    fun parents(clazz: KtClass, resolvedType: MutableMap<String, LClass<KtClass>>): List<LClass<KtClass>> {
        val mappedSuperclass = FqName(MappedSuperclass::class.qualifiedName!!)
        return clazz.superTypeListEntries
            .mapNotNull { it.analyze(BodyResolveMode.PARTIAL)[BindingContext.TYPE, it.typeReference]?.toClassDescriptor }
            .filter { it.annotations.hasAnnotation(mappedSuperclass) }
            .mapNotNull { DescriptorToSourceUtils.getSourceFromDescriptor(it) as? KtClass }
            .map { clazz(it, resolvedType) }
    }

    fun children(clazz: KtClass, resolvedType: MutableMap<String, LClass<KtClass>>): List<LClass<KtClass>> {
        val lightClass = clazz.toLightClass() ?: return emptyList()
        return ClassInheritorsSearch.search(lightClass, ProjectScope.getAllScope(clazz.project), false)
            .mapNotNull { psiClass ->
                val ktClass = (psiClass as? KtLightClass)?.kotlinOrigin as? KtClass
                    ?: return@mapNotNull null
                clazz(ktClass, resolvedType)
            }
    }

    fun properties(clazz: KtClass, containingLClass: LClass<KtClass>, resolvedType: MutableMap<String, LClass<KtClass>>): List<LProperty<*>> {
        return clazz.getProperties().map { resolve(it, containingLClass, resolvedType) }
    }

    fun methods(clazz: KtClass, resolvedType: MutableMap<String, LClass<KtClass>>): List<LMethod<*>> {
        val classBody = clazz.body ?: return emptyList()
        return classBody
            .functions
            .map { function ->
                val context = function.analyze(BodyResolveMode.PARTIAL)

                val params = function.valueParameters
                    .map { LParam(it.name!!, resolve(context[BindingContext.TYPE, it.typeReference]!!, resolvedType), it) }
                val annotations = function.annotationEntries.map { resolve(it, resolvedType) }
                val returnType = context[BindingContext.FUNCTION, function]?.returnType!!

                LMethod(
                    function.name!!,
                    annotations,
                    params,
                    LMethod.LReturnType(
                        resolve(returnType, resolvedType),
                        function.typeReference?.annotationEntries?.map { resolve(it, resolvedType) } ?: emptyList(),
                        annotations,
                    ),
                    function,
                )
            }
    }

    override fun resolve(element: PsiElement): LAnnotationOwner? {
        return when (element) {
            is KtClass -> clazz(element, mutableMapOf())
            is KtProperty -> {
                val owner = clazz(element.containingClass()!!, mutableMapOf())
                owner.allProperties.first { it.name == element.name }
            }

            is KtLightClass -> {
                val ktClass = element.kotlinOrigin as? KtClass ?: return null
                clazz(ktClass, mutableMapOf())
            }

            else -> null
        }
    }

    fun resolve(property: KtProperty, containingLClass: LClass<KtClass>, resolvedType: MutableMap<String, LClass<KtClass>>): LProperty<*> {
        val annotations = property.annotationEntries.map { resolve(it, resolvedType) }
        val type = (property.resolveToDescriptorIfAny() as? CallableDescriptor)?.returnType!!
        return LProperty(property.name!!, annotations, resolve(type, resolvedType), property, containingLClass)
    }

    fun resolve(type: KotlinType, resolvedType: MutableMap<String, LClass<KtClass>>): LType {
        val builtIns = DefaultBuiltIns.Instance

        val nullable = type.isMarkedNullable
        val descriptor = type.toClassDescriptor!!
        val ktClass = DescriptorToSourceUtils.getSourceFromDescriptor(descriptor) as? KtClass
        val fqName = descriptor.fqNameSafe.asString()
        val name = descriptor.name.asString()

        return when {
            KotlinBuiltIns.isPrimitiveType(type) -> LType.ScalarType(name, nullable)

            KotlinBuiltIns.isArray(type) -> LType.ArrayType(nullable, resolve(builtIns.getArrayElementType(type), resolvedType))

            else -> {
                when {
                    descriptor.kind.isEnumClass -> {
                        LType.EnumType(
                            name,
                            fqName,
                            nullable,
                            descriptor.unsubstitutedMemberScope
                                .getContributedDescriptors()
                                .filterIsInstance<ClassDescriptor>()
                                .filter { it.kind == ClassKind.ENUM_ENTRY }
                                .associate { it.name.asString() to DescriptorToSourceUtils.getSourceFromDescriptor(it)!! },
                            DescriptorToSourceUtils.getSourceFromDescriptor(descriptor)!!,
                        )
                    }

                    descriptor.kind.isClass || descriptor.kind.isInterface -> {
                        when (fqName) {
                            "kotlin.collections.List", "java.util.List" -> {
                                val argType = type.arguments.first().type
                                LType.CollectionType(
                                    nullable,
                                    resolve(argType, resolvedType),
                                    LType.CollectionType.CollectionKind.List,
                                )
                            }

                            "kotlin.collections.Set", "java.util.Set" -> {
                                val argType = type.arguments.first().type
                                LType.CollectionType(
                                    nullable,
                                    resolve(argType, resolvedType),
                                    LType.CollectionType.CollectionKind.Set,
                                )
                            }

                            "kotlin.collections.Map", "java.util.Map" -> {
                                val keyType = type.arguments[0].type
                                val valueType = type.arguments[1].type
                                LType.MapType(
                                    nullable,
                                    resolve(keyType, resolvedType),
                                    resolve(valueType, resolvedType),
                                )
                            }

                            else -> {
                                if (type.isInSource && ktClass != null) {
                                    clazz(ktClass, resolvedType)
                                } else {
                                    LType.ScalarType(name, nullable)
                                }
                            }
                        }
                    }

                    type.isInSource && ktClass != null -> clazz(ktClass, resolvedType)

                    else -> LType.ScalarType(name, nullable)
                }
            }
        }
    }

    fun resolve(annotation: KtAnnotationEntry, resolvedType: MutableMap<String, LClass<KtClass>>): LAnnotation<*> {
        val qualifiedName = annotation.qualifiedName
        val annotationType = annotation.analyze(BodyResolveMode.PARTIAL)[BindingContext.TYPE, annotation.typeReference]
        annotationType ?: throw IllegalStateException("KtAnnotationEntry must resolve to a KotlinType")
        val descriptor = annotationType.toClassDescriptor!!

        val params = descriptor
            .unsubstitutedMemberScope
            .getContributedDescriptors()
            .filterIsInstance<PropertyDescriptor>()
            .map { LParam(it.name.asString(), resolve(it.type, resolvedType), DescriptorToSourceUtils.getSourceFromDescriptor(it)!!) }

        return LAnnotation(
            qualifiedName.substringAfterLast('.'),
            qualifiedName,
            DescriptorToSourceUtils.getSourceFromDescriptor(descriptor)!!,
            params,
        )
    }
}
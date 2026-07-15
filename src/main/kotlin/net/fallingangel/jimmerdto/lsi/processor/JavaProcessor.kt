package net.fallingangel.jimmerdto.lsi.processor

import com.intellij.lang.java.JavaLanguage
import com.intellij.psi.*
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.search.searches.ClassInheritorsSearch
import net.fallingangel.jimmerdto.lsi.*
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotation
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotationOwner
import net.fallingangel.jimmerdto.lsi.param.LParam
import net.fallingangel.jimmerdto.psi.DTOFile
import net.fallingangel.jimmerdto.util.hasAnnotation
import net.fallingangel.jimmerdto.util.isInSource
import net.fallingangel.jimmerdto.util.nullable
import net.fallingangel.jimmerdto.util.psiClass
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.sql.Embeddable
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.MappedSuperclass

class JavaProcessor : LanguageProcessor<PsiClass> {
    override fun supports(dtoFile: DTOFile) = dtoFile.projectLanguage == JavaLanguage.INSTANCE

    override fun clazz(dtoFile: DTOFile): LClass<PsiClass> {
        val psiClass = dtoFile.project.psiClass(dtoFile.qualifiedEntity) ?: throw IllegalStateException("Entity class for $dtoFile not found")
        return clazz(psiClass, mutableMapOf())
    }

    fun clazz(clazz: PsiClass, resolvedType: MutableMap<String, LClass<PsiClass>>): LClass<PsiClass> {
        val qualifiedName = clazz.qualifiedName!!
        val name = clazz.name!!
        val type = resolvedType.getOrPut(qualifiedName) {
            lateinit var lClass: LClass<PsiClass>
            lClass = LClass(
                name,
                qualifiedName,
                false,
                clazz.isAnnotationType,
                clazz.annotations.map { resolve(it, resolvedType) },
                lazy { parents(clazz, resolvedType) },
                lazy { children(clazz, resolvedType) },
                lazy { properties(clazz, lClass, resolvedType) },
                lazy { methods(clazz, resolvedType) },
                clazz,
            )
            lClass
        }
        return type
    }

    fun parents(clazz: PsiClass, resolvedType: MutableMap<String, LClass<PsiClass>>): List<LClass<PsiClass>> {
        return clazz.supers
            .filter { it.qualifiedName != "java.lang.Object" }
            .filter { it.hasAnnotation(MappedSuperclass::class, Entity::class) }
            .map { clazz(it, resolvedType) }
    }

    fun children(clazz: PsiClass, resolvedType: MutableMap<String, LClass<PsiClass>>): List<LClass<PsiClass>> {
        return ClassInheritorsSearch.search(clazz, ProjectScope.getAllScope(clazz.project), false)
            .mapNotNull { clazz(it, resolvedType) }
    }

    fun properties(clazz: PsiClass, containingLClass: LClass<PsiClass>, resolvedType: MutableMap<String, LClass<PsiClass>>): List<LProperty<*>> {
        return if (clazz.hasAnnotation(Immutable::class, Entity::class, Embeddable::class, MappedSuperclass::class)) {
            clazz.methods
                .filter { !it.isConstructor }
                .map { resolve(it, containingLClass, resolvedType) }
        } else {
            clazz.fields
                .map { field ->
                    val annotations = field.annotations.map { resolve(it, resolvedType) }
                    LProperty(field.name, annotations, resolve(field.type, resolvedType), field, containingLClass)
                }
        }
    }

    fun methods(clazz: PsiClass, resolvedType: MutableMap<String, LClass<PsiClass>>): List<LMethod<*>> {
        return if (clazz.hasAnnotation(Immutable::class, Entity::class, Embeddable::class, MappedSuperclass::class)) {
            emptyList()
        } else {
            clazz.methods
                .filter { !it.isConstructor }
                .map { method ->
                    val params = method.parameterList.parameters.map { LParam(it.name, resolve(it.type, resolvedType), it) }
                    val annotations = method.annotations.map { resolve(it, resolvedType) }
                    val returnType = method.returnType ?: throw IllegalStateException("Method must have return type")

                    LMethod(
                        method.name,
                        annotations,
                        params,
                        LMethod.LReturnType(
                            resolve(returnType, resolvedType),
                            returnType.annotations.map { resolve(it, resolvedType) },
                            annotations,
                        ),
                        method,
                    )
                }
        }
    }

    override fun resolve(element: PsiElement): LAnnotationOwner? {
        return when (element) {
            is PsiClass -> clazz(element, mutableMapOf())
            is PsiMethod -> {
                val owner = clazz(element.containingClass!!, mutableMapOf())
                owner.allProperties.first { it.name == element.name }
            }

            else -> null
        }
    }

    fun resolve(method: PsiMethod, containingLClass: LClass<PsiClass>, resolvedType: MutableMap<String, LClass<PsiClass>>): LProperty<*> {
        val annotations = method.annotations.map { resolve(it, resolvedType) }
        // TODO 属性类型为Boolean时，jimmer.keepIsPrefix
        val methodName = method.name
        val name = if (methodName.startsWith("get") && methodName.length > 3 && methodName[3].isUpperCase()) {
            methodName[3].lowercaseChar() + methodName.substring(4)
        } else {
            methodName
        }

        return LProperty(name, annotations, resolve(method.returnType!!, resolvedType), method, containingLClass)
    }

    fun resolve(type: PsiType, resolvedType: MutableMap<String, LClass<PsiClass>>): LType {
        return when (type) {
            is PsiPrimitiveType -> LType.ScalarType(type.name, type.nullable)

            is PsiArrayType -> {
                val componentType = type.componentType
                LType.ArrayType(componentType.nullable, resolve(componentType, resolvedType))
            }

            is PsiClassType -> {
                val typeClass = type.resolve() ?: return LType.ScalarType(type.name, type.nullable)
                when {
                    typeClass.isEnum -> {
                        LType.EnumType(
                            type.name,
                            type.canonicalText,
                            false,
                            typeClass.fields
                                .filterIsInstance<PsiEnumConstant>()
                                .associateBy { it.name },
                            typeClass,
                        )
                    }

                    typeClass.isInSource -> clazz(typeClass, resolvedType)

                    type.hasParameters() -> {
                        val typeParameters = type.parameters
                        val rawType = type.rawType()
                        val type0 = typeParameters[0]
                        when {
                            rawType.canonicalText == "java.util.List" -> LType.CollectionType(
                                type0.nullable,
                                resolve(type0, resolvedType),
                                LType.CollectionType.CollectionKind.List,
                            )

                            rawType.canonicalText == "java.util.Queue" -> LType.CollectionType(
                                type0.nullable,
                                resolve(type0, resolvedType),
                                LType.CollectionType.CollectionKind.Queue,
                            )

                            rawType.canonicalText == "java.util.Set" -> LType.CollectionType(
                                type0.nullable,
                                resolve(type0, resolvedType),
                                LType.CollectionType.CollectionKind.Set,
                            )

                            rawType.canonicalText == "java.util.Map" -> LType.MapType(
                                false,
                                resolve(type0, resolvedType),
                                resolve(typeParameters[1], resolvedType),
                            )

                            rawType.resolve()!!.isInSource -> clazz(typeClass, resolvedType)

                            else -> LType.ScalarType(type.name, type.nullable)
                        }
                    }

                    else -> LType.ScalarType(type.name, type.nullable)
                }
            }

            else -> throw IllegalStateException("Unsupported PsiType: $type")
        }
    }

    fun resolve(annotation: PsiAnnotation, resolvedType: MutableMap<String, LClass<PsiClass>>): LAnnotation<*> {
        val clazz = annotation.resolveAnnotationType() ?: throw IllegalStateException("Can't find annotation type ${annotation.qualifiedName}")
        val methods = clazz.methods

        return LAnnotation(
            clazz.name!!,
            clazz.qualifiedName!!,
            clazz,
            methods.map { LParam(it.name, resolve(it.returnType!!, resolvedType), it) },
        )
    }
}
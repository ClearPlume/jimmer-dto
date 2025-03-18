package net.fallingangel.jimmerdto.lsi.processor

import com.intellij.lang.java.JavaLanguage
import com.intellij.psi.*
import net.fallingangel.jimmerdto.lsi.*
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotation
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

class JavaProcessor : LanguageProcessor<PsiClass, PsiAnnotation, PsiType> {
    override val resolvedType = mutableMapOf<String, LClass<PsiClass>>()

    override fun supports(dtoFile: DTOFile) = dtoFile.projectLanguage == JavaLanguage.INSTANCE

    override fun clazz(dtoFile: DTOFile): LClass<PsiClass> {
        val psiClass = dtoFile.project.psiClass(dtoFile.qualifiedEntity) ?: throw IllegalStateException("Entity class for $dtoFile not found")
        return clazz(psiClass)
    }

    override fun clazz(clazz: PsiClass): LClass<PsiClass> {
        val qualifiedName = clazz.qualifiedName!!
        val name = clazz.name!!
        val type = resolvedType.getOrPut(qualifiedName) {
            LClass(
                name,
                qualifiedName,
                false,
                clazz.annotations.map(::resolve),
                lazy { parents(clazz) },
                lazy { properties(clazz) },
                lazy { methods(clazz) },
                clazz,
            )
        }
        return type
    }

    override fun parents(clazz: PsiClass): List<LClass<PsiClass>> {
        return clazz.supers
                .filter { it.qualifiedName != "java.lang.Object" }
                .filter { it.hasAnnotation(MappedSuperclass::class) }
                .map(::clazz)
    }

    override fun properties(clazz: PsiClass): List<LProperty<*>> {
        return if (clazz.hasAnnotation(Immutable::class, Entity::class, Embeddable::class, MappedSuperclass::class)) {
            clazz.methods
                    .filter { !it.isConstructor }
                    .map {
                        val annotations = it.annotations.map(::resolve)
                        LProperty(it.name, annotations, resolve(it.returnType!!), it)
                    }
        } else {
            clazz.fields
                    .map {
                        val annotations = it.annotations.map(::resolve)
                        LProperty(it.name, annotations, resolve(it.type), it)
                    }
        }
    }

    override fun methods(clazz: PsiClass): List<LMethod<*>> {
        return if (clazz.hasAnnotation(Immutable::class, Entity::class, Embeddable::class, MappedSuperclass::class)) {
            emptyList()
        } else {
            clazz.methods
                    .filter { !it.isConstructor }
                    .map { method ->
                        val params = method.parameterList.parameters.map { LParam(it.name, resolve(it.type), it) }
                        val annotations = method.annotations.map(::resolve)
                        val returnType = method.returnType ?: throw IllegalStateException("Method must have return type")

                        LMethod(
                            method.name,
                            annotations,
                            params,
                            LMethod.LReturnType(
                                resolve(returnType),
                                returnType.annotations.map(::resolve),
                                annotations,
                            ),
                            method,
                        )
                    }
        }
    }

    override fun resolve(type: PsiType): LType {
        return when (type) {
            is PsiPrimitiveType -> LType.ScalarType(type.name, type.nullable)

            is PsiArrayType -> {
                val componentType = type.componentType
                LType.ArrayType(componentType.nullable, resolve(componentType))
            }

            is PsiClassType -> {
                val typeClass = type.resolve() ?: throw IllegalStateException("PsiClassType must resolve to a PsiClass")
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

                    typeClass.isInSource -> clazz(typeClass)

                    type.hasParameters() -> {
                        val typeParameters = type.parameters
                        val rawType = type.rawType()
                        val type0 = typeParameters[0]
                        when {
                            rawType.canonicalText == "java.util.List" -> LType.CollectionType(
                                type0.nullable,
                                resolve(type0),
                                LType.CollectionType.CollectionKind.List,
                            )

                            rawType.canonicalText == "java.util.Queue" -> LType.CollectionType(
                                type0.nullable,
                                resolve(type0),
                                LType.CollectionType.CollectionKind.Queue,
                            )

                            rawType.canonicalText == "java.util.Set" -> LType.CollectionType(
                                type0.nullable,
                                resolve(type0),
                                LType.CollectionType.CollectionKind.Set,
                            )

                            rawType.canonicalText == "java.util.Map" -> LType.MapType(
                                false,
                                resolve(type0),
                                resolve(typeParameters[1]),
                            )

                            rawType.resolve()!!.isInSource -> clazz(typeClass)

                            else -> LType.ScalarType(type.name, type.nullable)
                        }
                    }

                    else -> LType.ScalarType(type.name, type.nullable)
                }
            }

            else -> throw IllegalStateException("Unsupported PsiType: $type")
        }
    }

    override fun resolve(annotation: PsiAnnotation): LAnnotation<*> {
        val qualifiedName = annotation.qualifiedName!!
        val methods = annotation.resolveAnnotationType()?.methods ?: throw IllegalStateException("PsiAnnotation must resolve to a PsiClass")

        return LAnnotation(
            qualifiedName.substringAfterLast('.'),
            qualifiedName,
            annotation.resolveAnnotationType(),
            methods.map { LParam(it.name, resolve(it.returnType!!), it) },
        )
    }
}
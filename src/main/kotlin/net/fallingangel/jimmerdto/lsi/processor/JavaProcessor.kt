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
import net.fallingangel.jimmerdto.lsi.annotation.resolveAnnotation
import net.fallingangel.jimmerdto.psi.DTOFile
import net.fallingangel.jimmerdto.util.hasAnnotation
import net.fallingangel.jimmerdto.util.nullable
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.sql.Embeddable
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.MappedSuperclass

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

    override fun supports(dtoFile: DTOFile) = dtoFile.projectLanguage == JavaLanguage.INSTANCE

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
}
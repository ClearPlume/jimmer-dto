package net.fallingangel.jimmerdto.lsi.processor

import com.intellij.psi.PsiElement
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.search.searches.ClassInheritorsSearch
import net.fallingangel.jimmerdto.exception.IllegalTypeException
import net.fallingangel.jimmerdto.lsi.*
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotation
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotationOwner
import net.fallingangel.jimmerdto.lsi.param.LParam
import net.fallingangel.jimmerdto.psi.DTOFile
import net.fallingangel.jimmerdto.util.contains
import net.fallingangel.jimmerdto.util.isInSource
import net.fallingangel.jimmerdto.util.ktClass
import org.babyfish.jimmer.sql.MappedSuperclass
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.containingClass

class KotlinProcessor : LanguageProcessor<KtClass> {
    override fun supports(dtoFile: DTOFile) = dtoFile.projectLanguage == KotlinLanguage.INSTANCE

    override fun clazz(dtoFile: DTOFile): LClass<KtClass> {
        val ktClass = dtoFile.project.ktClass(dtoFile.qualifiedEntity).getOrNull(0)
        ktClass ?: throw IllegalStateException("Entity class for $dtoFile not found")
        return clazz(ktClass, mutableMapOf())
    }

    fun clazz(clazz: KtClass, resolvedType: MutableMap<String, LClass<KtClass>>): LClass<KtClass> {
        val annotations = analyze(clazz) {
            clazz.symbol.annotations.map { resolve(it, resolvedType) }
        }

        val qualifiedName = clazz.fqName?.asString()!!
        return resolvedType.getOrPut(qualifiedName) {
            lateinit var lClass: LClass<KtClass>
            lClass = LClass(
                clazz.name!!,
                qualifiedName,
                false,
                clazz.isAnnotation(),
                annotations,
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
        return analyze(clazz) {
            val symbol = clazz.symbol as? KaClassSymbol ?: return emptyList()
            symbol.superTypes
                .filter { MappedSuperclass::class in it.symbol!!.annotations }
                .mapNotNull { it.symbol?.psi as? KtClass }
                .map { clazz(it, resolvedType) }
        }
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
                analyze(function) {
                    val symbol = function.symbol
                    val annotations = symbol.annotations.map { resolve(it, resolvedType) }
                    symbol.valueParameters.forEach { resolve(it, resolvedType) }

                    LMethod(
                        function.name!!,
                        annotations,
                        symbol.valueParameters.map { resolve(it, resolvedType) },
                        LMethod.LReturnType(
                            resolve(symbol.returnType, resolvedType),
                            function.returnType.annotations.map { resolve(it, resolvedType) },
                            annotations,
                        ),
                        function,
                    )
                }
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
        return analyze(property) {
            val annotations = property.symbol.annotations.map { resolve(it, resolvedType) }
            val type = resolve(property.symbol.returnType, resolvedType)
            LProperty(property.name!!, annotations, type, property, containingLClass)
        }
    }

    fun KaSession.resolve(type: KaType, resolvedType: MutableMap<String, LClass<KtClass>>): LType {
        val nullable = type.isMarkedNullable
        val symbol = type.symbol ?: throw IllegalTypeException(type.toString())
        val classId = symbol.classId!!
        val name = symbol.name!!.asString()
        val fqName = classId.asFqNameString()

        return when {
            symbol is KaClassSymbol && symbol.classKind == KaClassKind.ENUM_CLASS -> {
                LType.EnumType(
                    name,
                    fqName,
                    nullable,
                    symbol.staticDeclaredMemberScope
                        .declarations
                        .filterIsInstance<KaEnumEntrySymbol>()
                        .associate { it.name.asString() to it.psi!! },
                    symbol.psi!!,
                )
            }

            type is KaClassType -> when (fqName) {
                "kotlin.collections.List", "java.util.List" -> {
                    val argType = type.typeArguments.first()
                    LType.CollectionType(
                        nullable,
                        resolve(argType, resolvedType),
                        LType.CollectionType.CollectionKind.List,
                    )
                }

                "kotlin.collections.Set", "java.util.Set" -> {
                    val argType = type.typeArguments.first()
                    LType.CollectionType(
                        nullable,
                        resolve(argType, resolvedType),
                        LType.CollectionType.CollectionKind.Set,
                    )
                }

                "kotlin.collections.Map", "java.util.Map" -> {
                    val keyType = type.typeArguments[0]
                    val valueType = type.typeArguments[1]
                    LType.MapType(
                        nullable,
                        resolve(keyType, resolvedType),
                        resolve(valueType, resolvedType),
                    )
                }

                else -> if (type.isInSource) {
                    when (val psi = symbol.psi) {
                        is KtClass -> clazz(psi, resolvedType)
                        else -> LType.ScalarType(name, nullable)
                    }
                } else {
                    LType.ScalarType(name, nullable)
                }
            }

            else -> LType.ScalarType(name, nullable)
        }
    }

    fun KaSession.resolve(type: KaTypeProjection, resolvedType: MutableMap<String, LClass<KtClass>>): LType {
        return when (type) {
            is KaStarTypeProjection -> LType.ScalarType("*", false)
            is KaTypeArgumentWithVariance -> resolve(type.type, resolvedType)
        }
    }

    fun KaSession.resolve(annotation: KaAnnotation, resolvedType: MutableMap<String, LClass<KtClass>>): LAnnotation<*> {
        val constructor = annotation.constructorSymbol!!
        val classId = annotation.classId!!
        return LAnnotation(
            classId.shortClassName.asString(),
            classId.asFqNameString(),
            annotation.psi!!,
            constructor.valueParameters.map { resolve(it, resolvedType) },
        )
    }

    fun KaSession.resolve(param: KaValueParameterSymbol, resolvedType: MutableMap<String, LClass<KtClass>>): LParam<*> {
        return LParam(param.name.asString(), resolve(param.returnType, resolvedType), param.psi!!)
    }
}
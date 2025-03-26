package net.fallingangel.jimmerdto.lsi.processor

import net.fallingangel.jimmerdto.lsi.*
import net.fallingangel.jimmerdto.lsi.annotation.LAnnotation
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
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtClass

class KotlinProcessor : LanguageProcessor<KtClass> {
    override val resolvedType = mutableMapOf<String, LClass<KtClass>>()

    override fun supports(dtoFile: DTOFile) = dtoFile.projectLanguage == KotlinLanguage.INSTANCE

    override fun clazz(dtoFile: DTOFile): LClass<KtClass> {
        val ktClass = dtoFile.project.ktClass(dtoFile.qualifiedEntity).getOrNull(0)
        ktClass ?: throw IllegalStateException("Entity class for $dtoFile not found")
        return clazz(ktClass)
    }

    override fun clazz(clazz: KtClass): LClass<KtClass> {
        val annotations = analyze(clazz) {
            clazz.symbol.annotations.map { resolve(it) }
        }

        val qualifiedName = clazz.fqName?.asString()!!
        return resolvedType.getOrPut(qualifiedName) {
            LClass(
                clazz.name!!,
                qualifiedName,
                false,
                clazz.isAnnotation(),
                annotations,
                lazy { parents(clazz) },
                lazy { properties(clazz) },
                lazy { methods(clazz) },
                clazz,
            )
        }
    }

    override fun parents(clazz: KtClass): List<LClass<KtClass>> {
        return analyze(clazz) {
            val symbol = clazz.symbol as? KaClassSymbol ?: return emptyList()
            symbol.superTypes
                    .filter { MappedSuperclass::class in it.symbol!!.annotations }
                    .mapNotNull { it.symbol?.psi as? KtClass }
                    .map(::clazz)
        }
    }

    override fun properties(clazz: KtClass): List<LProperty<*>> {
        return clazz.getProperties()
                .map { property ->
                    analyze(property) {
                        val annotations = property.symbol.annotations.map { resolve(it) }
                        val type = resolve(property.symbol.returnType)
                        LProperty(property.name!!, annotations, type, property)
                    }
                }
    }

    override fun methods(clazz: KtClass): List<LMethod<*>> {
        val classBody = clazz.body ?: return emptyList()
        return classBody
                .functions
                .map { function ->
                    analyze(function) {
                        val symbol = function.symbol
                        val annotations = symbol.annotations.map { resolve(it) }
                        symbol.valueParameters.map { resolve(it) }

                        LMethod(
                            function.name!!,
                            annotations,
                            symbol.valueParameters.map { resolve(it) },
                            LMethod.LReturnType(
                                resolve(symbol.returnType),
                                function.returnType.annotations.map { resolve(it) },
                                annotations,
                            ),
                            function,
                        )
                    }
                }
    }

    fun KaSession.resolve(type: KaType): LType {
        val nullable = type.isMarkedNullable
        val symbol = type.symbol!!
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
                        resolve(argType),
                        LType.CollectionType.CollectionKind.List,
                    )
                }

                "kotlin.collections.Set", "java.util.Set" -> {
                    val argType = type.typeArguments.first()
                    LType.CollectionType(
                        nullable,
                        resolve(argType),
                        LType.CollectionType.CollectionKind.Set,
                    )
                }

                "kotlin.collections.Map", "java.util.Map" -> {
                    val keyType = type.typeArguments[0]
                    val valueType = type.typeArguments[1]
                    LType.MapType(
                        nullable,
                        resolve(keyType),
                        resolve(valueType),
                    )
                }

                else -> if (type.isInSource) {
                    when (val psi = symbol.psi) {
                        is KtClass -> clazz(psi)
                        else -> LType.ScalarType(name, nullable)
                    }
                } else {
                    LType.ScalarType(name, nullable)
                }
            }

            else -> LType.ScalarType(name, nullable)
        }
    }

    fun KaSession.resolve(type: KaTypeProjection): LType {
        return when (type) {
            is KaStarTypeProjection -> LType.ScalarType("*", false)
            is KaTypeArgumentWithVariance -> resolve(type.type)
        }
    }

    fun KaSession.resolve(annotation: KaAnnotation): LAnnotation<*> {
        val constructor = annotation.constructorSymbol!!
        val classId = annotation.classId!!
        return LAnnotation(
            classId.shortClassName.asString(),
            classId.asFqNameString(),
            annotation.psi,
            constructor.valueParameters.map { resolve(it) },
        )
    }

    fun KaSession.resolve(param: KaValueParameterSymbol): LParam<*> {
        return LParam(param.name.asString(), resolve(param.returnType), param.psi)
    }
}
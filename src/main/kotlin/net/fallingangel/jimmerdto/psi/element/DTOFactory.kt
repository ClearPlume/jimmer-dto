package net.fallingangel.jimmerdto.psi.element

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import net.fallingangel.jimmerdto.DTOLanguage
import net.fallingangel.jimmerdto.psi.DTOFile
import net.fallingangel.jimmerdto.util.findChild

fun Project.createDTOFile(content: String = ""): DTOFile {
    return PsiFileFactory.getInstance(this).createFileFromText(DTOLanguage, content) as DTOFile
}

fun Project.createComma(): PsiElement {
    return createDTOFile(",")
            .firstChild
            .firstChild
            .firstChild
}

fun Project.createImport(qualifiedName: String): DTOImportStatement {
    return createDTOFile("import $qualifiedName").findChild("/dtoFile/importStatement")
}

fun Project.createQualifiedNamePart(part: String): DTOQualifiedNamePart {
    return createImport(part).qualifiedName.parts[0]
}

fun Project.createImported(type: String): DTOImported {
    return createDTOFile("import a.{$type}").findChild("/dtoFile/importStatement/groupedImport/importedType/imported")
}

fun Project.createDTO(
    name: String,
    modifiers: List<String> = emptyList(),
    implements: List<String> = emptyList(),
    userProps: List<String> = emptyList(),
    macros: List<String> = emptyList(),
    aliasGroups: List<String> = emptyList(),
    positiveProps: List<String> = emptyList(),
    negativeProps: List<String> = emptyList()
): DTODto {
    val modifiers = modifiers.joinToString(" ")
    val implement = if (implements.isEmpty()) {
        ""
    } else {
        implements.joinToString(System.lineSeparator(), "implement")
    }
    val userProp = userProps.joinToString(System.lineSeparator())
    val macro = macros.joinToString(System.lineSeparator())
    val aliasGroup = aliasGroups.joinToString(System.lineSeparator())
    val positiveProp = positiveProps.joinToString(System.lineSeparator())
    val negativeProp = negativeProps.joinToString(System.lineSeparator())

    val dtoFile = createDTOFile(
        """$modifiers $name $implement {
        |   $userProp
        |   $macro
        |   $aliasGroup
        |   $positiveProp
        |   $negativeProp
        |}""".trimMargin()
    )
    return dtoFile.findChild("/dtoFile/dto")
}

fun Project.createModifier(modifier: String): PsiElement {
    return createDTO("Dummy", modifiers = listOf(modifier)).modifierElements[0]
}

fun Project.createDTOName(name: String): DTODtoName {
    return createDTO(name).name
}

fun Project.createEnumMappingProp(name: String, mappings: List<String>): DTOPositiveProp {
    val mapping = mappings.joinToString(System.lineSeparator())
    val enumMapping = """$name -> {
        |   $mapping
        |}
    """.trimMargin()
    return createDTO("Dummy", positiveProps = listOf(enumMapping))
            .dtoBody
            .positiveProps[0]
}

fun Project.createEnumMappings(mappings: List<String>): List<DTOEnumMapping> {
    return createEnumMappingProp("dummy", mappings)
            .body!!
            .enumBody!!
            .mappings
}

fun Project.createEnumMapping(mapping: String): DTOEnumMapping {
    return createEnumMappingProp("dummy", listOf(mapping))
            .body!!
            .enumBody!!
            .mappings[0]
}

fun Project.createUserProp(name: String, type: String): DTOUserProp {
    return createDTO("Dummy", userProps = listOf("$name: $type"))
            .dtoBody
            .userProps[0]
}

fun Project.createUserPropType(type: String) = createUserProp("dummy", type).type

fun Project.createPropName(name: String): DTOPropName {
    return createDTO("Dummy", positiveProps = listOf(name))
            .dtoBody
            .positiveProps[0]
            .name
}

fun Project.createAlias(alias: String): DTOAlias {
    return createDTO("Dummy", positiveProps = listOf("dummy as $alias"))
            .dtoBody
            .positiveProps[0]
            .alias!!
}

fun Project.createAnnotation(name: String, params: List<String> = emptyList()): DTOAnnotation {
    val prop = if (params.isEmpty()) {
        """@$name
        dummy
        """.trimIndent()
    } else {
        val paramStr = params.joinToString(prefix = "(", postfix = ")")
        """@$name$paramStr
        dummy
        """.trimIndent()
    }
    return createDTO("Dummy", positiveProps = listOf(prop))
            .dtoBody
            .positiveProps[0]
            .annotations[0]
}

fun Project.createAnnotationValue(value: String): DTOAnnotationValue {
    return createAnnotation("Dummy", listOf(value)).value!!
}

fun Project.createAnnotationParameter(name: String, value: String = "dummy"): DTOAnnotationParameter {
    return createAnnotation("Dummy", listOf("$name = $value"))
            .params[0]
}

fun Project.createValue(value: String): DTOValue {
    return createDTO("Dummy", positiveProps = listOf("id($value)"))
            .dtoBody
            .positiveProps[0]
            .arg!!
            .values[0]
}

fun Project.createMacro(name: String, argList: List<String> = emptyList()): DTOMacro {
    val args = if (argList.isNotEmpty()) {
        argList.joinToString(separator = ",", prefix = "(", postfix = ")")
    } else {
        ""
    }
    return createDTO("Dummy", macros = listOf("#$name$args"))
            .dtoBody
            .macros[0]
}

fun Project.createMacroName(name: String) = createMacro(name).name

fun Project.createMacroArg(arg: String): DTOMacroArg {
    return createMacro("allScalars", listOf(arg))
            .args!!
            .values[0]
}
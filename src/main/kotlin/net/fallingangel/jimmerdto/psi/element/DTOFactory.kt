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
    implements: List<String> = emptyList(),
    userProps: List<String> = emptyList(),
    macros: List<String> = emptyList(),
    aliasGroups: List<String> = emptyList(),
    positiveProps: List<String> = emptyList(),
    negativeProps: List<String> = emptyList()
): DTODto {
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
        """$name $implement {
        |   $userProp
        |   $macro
        |   $aliasGroup
        |   $positiveProp
        |   $negativeProp
        |}""".trimMargin()
    )
    return dtoFile.findChild("/dtoFile/dto")
}

// fun Project.createDTOName(name: String): DTODtoName {
//     return createDTO(name).name
// }

// fun Project.createAliasGroup(
//     pattern: String,
//     userProps: List<String> = emptyList(),
//     macros: List<String> = emptyList()
// ): DTOAliasGroup {
//     val userProp = userProps.joinToString(System.lineSeparator())
//     val macro = macros.joinToString(System.lineSeparator())
//
//     val aliasGroup = """as($pattern) {
//         |   $userProp
//         |   $macro
//         |}""".trimMargin()
//     return createDTO("Dummy", aliasGroups = listOf(aliasGroup))
//             .dtoBody
//             .aliasGroups[0]
// }

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

// fun Project.createAnnotation(name: String): DTOAnnotation {
//     val prop = """@$name
//         dummy
//     """.trimIndent()
//     return createDTO("Dummy", positiveProps = listOf(prop))
//             .dtoBody
//             .positiveProps[0]
//             .annotations[0]
// }

// fun Project.createValue(value: String): DTOValue {
//     return createDTO("Dummy", positiveProps = listOf("id($value)"))
//             .dtoBody
//             .positiveProps[0]
//             .arg!!
//             .values[0]
// }

fun Project.createMacro(argList: List<String> = emptyList()): DTOMacro {
    val args = if (argList.isNotEmpty()) {
        argList.joinToString(separator = ",", prefix = "(", postfix = ")")
    } else {
        ""
    }
    return createDTO("Dummy", macros = listOf("#allScalars$args"))
            .dtoBody
            .macros[0]
}

fun Project.createMacroName() = createMacro().name

fun Project.createMacroArg(arg: String): PsiElement {
    return createMacro(listOf(arg))
            .args!!
            .values[0]
}
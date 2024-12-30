package net.fallingangel.jimmerdto.psi

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import net.fallingangel.jimmerdto.DTOLanguage
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType

fun Project.createDTOFile(content: String = ""): DTOFile {
    return PsiFileFactory.getInstance(this).createFileFromText(DTOLanguage.INSTANCE, content) as DTOFile
}

fun Project.createImport(qualifiedName: String): DTOImport {
    return createDTOFile("import $qualifiedName").getChildOfType()!!
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

    return createDTOFile(
        """$name $implement {
        |   $userProp
        |   $macro
        |   $aliasGroup
        |   $positiveProp
        |   $negativeProp
        |}""".trimMargin()
    ).getChildOfType()!!
}

fun Project.createAliasGroup(
    pattern: String,
    userProps: List<String> = emptyList(),
    macros: List<String> = emptyList()
): DTOAliasGroup {
    val userProp = userProps.joinToString(System.lineSeparator())
    val macro = macros.joinToString(System.lineSeparator())

    val aliasGroup = """as($pattern) {
        |   $userProp
        |   $macro
        |}""".trimMargin()
    return createDTO("Dummy", aliasGroups = listOf(aliasGroup))
            .dtoBody!!
            .explicitPropList[0]
            .aliasGroup!!
}

/**
 * @return alias-pattern中的合法『replacement』元素。因为没有对应的Psi类定义，只能以[PsiElement]类型返回
 */
fun Project.createAliasGroupReplacement(replacement: String = ""): PsiElement? {
    return createAliasGroup("^ -> $replacement").aliasPattern.replacement?.identifier
}

fun Project.createEnumMappingProp(name: String, mappings: List<String>): DTOPositiveProp {
    val mapping = mappings.joinToString(System.lineSeparator())
    val enumMapping = """$name -> {
        |   $mapping
        |}
    """.trimMargin()
    return createDTO("Dummy", positiveProps = listOf(enumMapping))
            .dtoBody!!
            .explicitPropList[0]
            .positiveProp!!
}

fun Project.createEnumMappings(mappings: List<String>): List<DTOEnumInstanceMapping> {
    return createEnumMappingProp("dummy", mappings)
            .enumBody!!
            .enumInstanceMappingList
}

fun Project.createUserProp(name: String, type: String): DTOUserProp {
    return createDTO("Dummy", userProps = listOf("$name: $type"))
            .dtoBody!!
            .explicitPropList[0]
            .userProp!!
}

fun Project.createUserPropName(name: String) = createUserProp(name, "String").propName

fun Project.createAnnotation(name: String): DTOAnnotation {
    val prop = """@$name
        dummy
    """.trimIndent()
    return createDTO("Dummy", positiveProps = listOf(prop))
            .dtoBody!!
            .explicitPropList[0]
            .positiveProp!!
            .annotationList[0]
}
package net.fallingangel.jimmerdto.psi

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import net.fallingangel.jimmerdto.DTOLanguage
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType

/**
 * 以指定内容创建[DTOFile]实例
 *
 * @param content DTO文件内容
 *
 * @return [DTOFile]实例
 */
fun Project.createDTOFile(content: String = ""): DTOFile {
    return PsiFileFactory.getInstance(this).createFileFromText(DTOLanguage.INSTANCE, content) as DTOFile
}

/**
 * 创建导入指定全限定类名的[DTOImport]实例
 *
 * @param qualifiedName 全限定类名
 *
 * @return [DTOImport]实例
 */
fun Project.createImport(qualifiedName: String): DTOImport {
    return createDTOFile("import $qualifiedName").getChildOfType()!!
}

/**
 * 以指定名称创建[DTODto]实例
 *
 * @param name Dto名称
 * @param userProps Dto元素
 * @param macros Dto元素
 * @param aliasGroups Dto元素
 * @param positiveProps Dto元素
 * @param negativeProps Dto元素
 *
 * @return [DTODto]实例
 */
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
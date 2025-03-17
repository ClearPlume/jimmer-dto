package net.fallingangel.jimmerdto

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import net.fallingangel.jimmerdto.psi.DTOFile
import net.fallingangel.jimmerdto.psi.DTOLexer
import net.fallingangel.jimmerdto.psi.DTOParser
import net.fallingangel.jimmerdto.psi.element.impl.*
import org.antlr.intellij.adaptor.lexer.PSIElementTypeFactory
import org.antlr.intellij.adaptor.lexer.RuleIElementType
import org.antlr.intellij.adaptor.lexer.TokenIElementType
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTOParserDefinition : ParserDefinition {
    override fun createLexer(project: Project) = DTOLexerAdapter()

    override fun createParser(project: Project) = DTOParserAdapter()

    override fun getFileNodeType() = Companion.FILE

    override fun getWhitespaceTokens(): TokenSet {
        return PSIElementTypeFactory.createTokenSet(
            DTOLanguage,
            DTOLexer.WhiteSpace,
        )
    }

    override fun getCommentTokens(): TokenSet {
        return PSIElementTypeFactory.createTokenSet(
            DTOLanguage,
            DTOLexer.LineComment,
            DTOLexer.BlockComment,
            DTOLexer.DocComment,
        )
    }

    override fun getStringLiteralElements(): TokenSet {
        return PSIElementTypeFactory.createTokenSet(
            DTOLanguage,
            DTOLexer.StringLiteral,
            DTOLexer.SqlStringLiteral,
        )
    }

    override fun createElement(node: ASTNode): PsiElement {
        val type = node.elementType

        if (type is TokenIElementType || type !is RuleIElementType) {
            return ANTLRPsiNode(node)
        }

        return when (type.ruleIndex) {
            DTOParser.RULE_exportStatement -> DTOExportStatementImpl(node)
            DTOParser.RULE_importStatement -> DTOImportStatementImpl(node)
            DTOParser.RULE_groupedImport -> DTOGroupedImportImpl(node)
            DTOParser.RULE_importedType -> DTOImportedTypeImpl(node)
            DTOParser.RULE_imported -> DTOImportedImpl(node)
            DTOParser.RULE_alias -> DTOAliasImpl(node)
            DTOParser.RULE_qualifiedName -> DTOQualifiedNameImpl(node)
            DTOParser.RULE_qualifiedNamePart -> DTOQualifiedNamePartImpl(node)

            DTOParser.RULE_dto -> DTODtoImpl(node)
            DTOParser.RULE_dtoName -> DTODtoNameImpl(node)
            DTOParser.RULE_implements -> DTOImplementsImpl(node)
            DTOParser.RULE_dtoBody -> DTODtoBodyImpl(node)

            DTOParser.RULE_annotation -> DTOAnnotationImpl(node)
            DTOParser.RULE_annotationValue -> DTOAnnotationValueImpl(node)
            DTOParser.RULE_annotationSingleValue -> DTOAnnotationSingleValueImpl(node)
            DTOParser.RULE_annotationArrayValue -> DTOAnnotationArrayValueImpl(node)
            DTOParser.RULE_annotationParameter -> DTOAnnotationParameterImpl(node)
            DTOParser.RULE_nestedAnnotation -> DTONestAnnotationImpl(node)

            DTOParser.RULE_macro -> DTOMacroImpl(node)
            DTOParser.RULE_macroName -> DTOMacroNameImpl(node)
            DTOParser.RULE_macroArgs -> DTOMacroArgsImpl(node)

            DTOParser.RULE_negativeProp -> DTONegativePropImpl(node)

            DTOParser.RULE_aliasGroup -> DTOAliasGroupImpl(node)

            DTOParser.RULE_userProp -> DTOUserPropImpl(node)
            DTOParser.RULE_typeRef -> DTOTypeDefImpl(node)
            DTOParser.RULE_genericArguments -> DTOGenericArgumentsImpl(node)
            DTOParser.RULE_genericArgument -> DTOGenericArgumentImpl(node)

            DTOParser.RULE_positiveProp -> DTOPositivePropImpl(node)
            DTOParser.RULE_propConfig -> DTOPropConfigImpl(node)
            DTOParser.RULE_whereArgs -> DTOWhereArgsImpl(node)
            DTOParser.RULE_predicate -> DTOPredicateImpl(node)
            DTOParser.RULE_compare -> DTOCompareImpl(node)
            DTOParser.RULE_compareSymbol -> DTOCompareSymbolImpl(node)
            DTOParser.RULE_nullity -> DTONullityImpl(node)
            DTOParser.RULE_orderByArgs -> DTOOrderByArgsImpl(node)
            DTOParser.RULE_orderItem -> DTOOrderItemImpl(node)
            DTOParser.RULE_propValue -> DTOPropValueImpl(node)
            DTOParser.RULE_intPair -> DTOIntPairImpl(node)
            DTOParser.RULE_propName -> DTOPropNameImpl(node)
            DTOParser.RULE_propFlag -> DTOPropFlagImpl(node)
            DTOParser.RULE_propArg -> DTOPropArgImpl(node)
            DTOParser.RULE_value -> DTOValueImpl(node)
            DTOParser.RULE_propBody -> DTOPropBodyImpl(node)
            DTOParser.RULE_enumBody -> DTOEnumBodyImpl(node)
            DTOParser.RULE_enumMapping -> DTOEnumMappingImpl(node)
            else -> ANTLRPsiNode(node)
        }
    }

    override fun createFile(viewProvider: FileViewProvider) = DTOFile(viewProvider)

    object Companion {
        val FILE = IFileElementType(DTOLanguage)
    }
}

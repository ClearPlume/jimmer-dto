package net.fallingangel.jimmerdto

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import icons.Icons
import net.fallingangel.jimmerdto.psi.DTOLexer
import net.fallingangel.jimmerdto.psi.DTOParser
import net.fallingangel.jimmerdto.psi.element.DTODtoName
import org.antlr.intellij.adaptor.lexer.RuleIElementType
import org.antlr.intellij.adaptor.lexer.TokenIElementType

class DTOLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(element: PsiElement, result: MutableCollection<in RelatedItemLineMarkerInfo<*>>) {
        // 针对DTO名称元素发起跳转
        val type = element.elementType
        val parentType = element.parent.elementType

        if (type !is TokenIElementType || parentType !is RuleIElementType) {
            return
        }

        if (type.antlrTokenType == DTOLexer.Identifier && parentType.ruleIndex == DTOParser.RULE_dtoName) {
            val dtoName = element.parent as? DTODtoName ?: return
            val dtoClass = dtoName.resolve() ?: return

            result.add(
                NavigationGutterIconBuilder.create(Icons.icon_16)
                        .setTargets(dtoClass)
                        .setTooltipText("Jump to generated class [${dtoName.value}]")
                        .createLineMarkerInfo(element)
            )
        }
    }
}

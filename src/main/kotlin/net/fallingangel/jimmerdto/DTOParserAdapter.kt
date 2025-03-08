package net.fallingangel.jimmerdto

import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType
import net.fallingangel.jimmerdto.psi.DTOParser
import org.antlr.intellij.adaptor.parser.ANTLRParserAdaptor
import org.antlr.v4.runtime.Parser
import org.antlr.v4.runtime.tree.ParseTree

class DTOParserAdapter : ANTLRParserAdaptor(DTOLanguage, DTOParser(null)) {
    override fun parse(parser: Parser?, root: IElementType?): ParseTree {
        if (root is IFileElementType) {
            return (parser as DTOParser).dtoFile()
        }
        throw UnsupportedOperationException("Can't parse ${root?.javaClass?.name}")
    }
}
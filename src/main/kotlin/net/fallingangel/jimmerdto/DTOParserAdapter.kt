package net.fallingangel.jimmerdto

import com.intellij.psi.tree.IElementType
import net.fallingangel.jimmerdto.psi.DTOParser
import org.antlr.intellij.adaptor.parser.ANTLRParserAdaptor
import org.antlr.v4.runtime.Parser
import org.antlr.v4.runtime.tree.ParseTree

class DTOParserAdapter : ANTLRParserAdaptor(DTOLanguage, DTOParser(null)) {
    override fun parse(parser: Parser?, root: IElementType?): ParseTree {
        if (parser !is DTOParser) {
            throw UnsupportedOperationException("Can't parse ${root?.javaClass?.name}")
        }

        parser.removeErrorListeners()
        parser.addErrorListener(DTOErrorListener())

        return parser.dtoFile()
    }
}
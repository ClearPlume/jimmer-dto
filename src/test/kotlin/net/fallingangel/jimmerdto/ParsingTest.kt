package net.fallingangel.jimmerdto

import com.intellij.testFramework.ParsingTestCase
import net.fallingangel.jimmerdto.core.DTOParserDefinition

class ParsingTest : ParsingTestCase("parsing", "dto", DTOParserDefinition()) {
    override fun getTestDataPath() = "src/test/resources"

    override fun includeRanges() = true

    fun testParsing() {
        doTest(true)
    }
}
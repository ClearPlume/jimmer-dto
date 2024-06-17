package net.fallingangel.jimmerdto

import com.intellij.testFramework.ParsingTestCase

class ParsingTest : ParsingTestCase("parsing", "dto", DTOParserDefinition()) {
    override fun getTestDataPath() = "src/test/resources"

    override fun includeRanges() = true

    fun testParsing() {
        doTest(true)
    }
}
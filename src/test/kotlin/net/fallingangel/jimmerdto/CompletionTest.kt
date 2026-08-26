package net.fallingangel.jimmerdto

import com.intellij.testFramework.IdeaTestUtil
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.CompletionAutoPopupTestCase
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor

class CompletionTest : CompletionAutoPopupTestCase() {
    override fun getTestDataPath() = "src/test"

    override fun getProjectDescriptor(): LightProjectDescriptor {
        return DefaultLightProjectDescriptor { IdeaTestUtil.getMockJdk21() }
    }

    fun testCompletion() {
        myFixture.configureByFiles("resources/completion/Completion.dto", "kotlin/net/fallingangel/jimmerdto/entity/Book.kt")
        myFixture.completeBasic()
        myTester.joinCompletion()
        val lookupElementStrings = myFixture.lookupElementStrings

        assertNotNull(lookupElementStrings)
        assertSameElements(
            lookupElementStrings!!,
            "#allScalars",
            "as() {}",
            "flat() {}",
            "id()",
            "id",
            "authorIds",
            "authors",
            "edition",
            "name",
            "price",
            "store",
            "storeId"
        )
    }
}
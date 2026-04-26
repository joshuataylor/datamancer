package com.github.joshuataylor.datamancer.dbtcore.documentation

import com.intellij.platform.backend.documentation.DocumentationTargetProvider
import com.intellij.platform.backend.documentation.PsiDocumentationTargetProvider
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for [DatamancerBuiltinDocumentationProvider].
 * Tests interface conformance and negative cases.
 */
class DatamancerBuiltinDocumentationProviderTest : BasePlatformTestCase() {

    // -- Interface conformance --

    fun testProviderCanBeInstantiated() {
        val provider = DatamancerBuiltinDocumentationProvider()
        assertNotNull(provider)
    }

    fun testProviderImplementsDocumentationTargetProvider() {
        val provider = DatamancerBuiltinDocumentationProvider()
        assertTrue(
            "Should implement DocumentationTargetProvider",
            provider is DocumentationTargetProvider
        )
    }

    fun testProviderImplementsPsiDocumentationTargetProvider() {
        val provider = DatamancerBuiltinDocumentationProvider()
        assertTrue(
            "Should implement PsiDocumentationTargetProvider",
            provider is PsiDocumentationTargetProvider
        )
    }

    // -- Negative cases: non-template files --

    fun testDocumentationTargetsReturnsEmptyForPlainTextFile() {
        val file = myFixture.configureByText("test.txt", "hello world")
        val provider = DatamancerBuiltinDocumentationProvider()
        val targets = provider.documentationTargets(file, 0)
        assertNotNull(targets)
        assertTrue("Should return empty list for plain text file", targets.isEmpty())
    }

    fun testDocumentationTargetsReturnsEmptyForJsonFile() {
        val file = myFixture.configureByText("test.json", """{"key": "value"}""")
        val provider = DatamancerBuiltinDocumentationProvider()
        val targets = provider.documentationTargets(file, 2)
        assertTrue("Should return empty list for JSON file", targets.isEmpty())
    }

    fun testDocumentationTargetsReturnsEmptyForYamlFile() {
        val file = myFixture.configureByText("schema.yml", "models:\n  - name: foo")
        val provider = DatamancerBuiltinDocumentationProvider()
        val targets = provider.documentationTargets(file, 0)
        assertTrue("Should return empty list for YAML file", targets.isEmpty())
    }

    // -- Negative cases: PSI element API --

    fun testDocumentationTargetReturnsNullForNonDbtElement() {
        val file = myFixture.configureByText("test.txt", "hello world")
        val element = file.findElementAt(0) ?: return
        val provider = DatamancerBuiltinDocumentationProvider()
        val target = provider.documentationTarget(element, element)
        assertNull("Should return null for non-dbt element", target)
    }

    // -- Negative cases: offset boundaries --

    fun testDocumentationTargetsReturnsEmptyForBeyondEndOffset() {
        val file = myFixture.configureByText("test.txt", "hi")
        val provider = DatamancerBuiltinDocumentationProvider()
        val targets = provider.documentationTargets(file, 999)
        assertTrue(targets.isEmpty())
    }
}

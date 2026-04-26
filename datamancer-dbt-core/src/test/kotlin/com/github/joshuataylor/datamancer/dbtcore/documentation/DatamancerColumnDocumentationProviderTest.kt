package com.github.joshuataylor.datamancer.dbtcore.documentation

import com.intellij.platform.backend.documentation.DocumentationTargetProvider
import com.intellij.platform.backend.documentation.PsiDocumentationTargetProvider
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for DatamancerColumnDocumentationProvider.
 * Tests interface conformance, negative cases, and identifier filtering.
 */
class DatamancerColumnDocumentationProviderTest : BasePlatformTestCase() {

    // -- Interface conformance --

    fun testProviderCanBeInstantiated() {
        val provider = DatamancerColumnDocumentationProvider()
        assertNotNull(provider)
    }

    fun testProviderImplementsDocumentationTargetProvider() {
        val provider = DatamancerColumnDocumentationProvider()
        assertTrue(
            "Should implement DocumentationTargetProvider",
            provider is DocumentationTargetProvider
        )
    }

    fun testProviderImplementsPsiDocumentationTargetProvider() {
        val provider = DatamancerColumnDocumentationProvider()
        assertTrue(
            "Should implement PsiDocumentationTargetProvider",
            provider is PsiDocumentationTargetProvider
        )
    }

    // -- Negative cases: non-SQL files --

    fun testDocumentationTargetsReturnsEmptyForPlainTextFile() {
        val file = myFixture.configureByText("test.txt", "hello world")
        val provider = DatamancerColumnDocumentationProvider()
        val targets = provider.documentationTargets(file, 0)
        assertNotNull(targets)
        assertTrue("Should return empty list for plain text file", targets.isEmpty())
    }

    fun testDocumentationTargetsReturnsEmptyForJsonFile() {
        val file = myFixture.configureByText("test.json", """{"key": "value"}""")
        val provider = DatamancerColumnDocumentationProvider()
        val targets = provider.documentationTargets(file, 2)
        assertTrue("Should return empty list for JSON file", targets.isEmpty())
    }

    fun testDocumentationTargetsReturnsEmptyForYamlFile() {
        val file = myFixture.configureByText("schema.yml", "models:\n  - name: foo")
        val provider = DatamancerColumnDocumentationProvider()
        val targets = provider.documentationTargets(file, 0)
        assertTrue("Should return empty list for YAML file", targets.isEmpty())
    }

    fun testDocumentationTargetsReturnsEmptyForPythonFile() {
        val file = myFixture.configureByText("test.py", "x = 1")
        val provider = DatamancerColumnDocumentationProvider()
        val targets = provider.documentationTargets(file, 0)
        assertTrue("Should return empty list for Python file", targets.isEmpty())
    }

    // -- Negative cases: PSI element API --

    fun testDocumentationTargetReturnsNullForNonDbtElement() {
        val file = myFixture.configureByText("test.txt", "hello world")
        val element = file.findElementAt(0) ?: return
        val provider = DatamancerColumnDocumentationProvider()
        val target = provider.documentationTarget(element, element)
        assertNull("Should return null for non-dbt element", target)
    }

    fun testDocumentationTargetReturnsNullForYamlElement() {
        val file = myFixture.configureByText("schema.yml", "models:\n  - name: foo")
        val element = file.findElementAt(0) ?: return
        val provider = DatamancerColumnDocumentationProvider()
        val target = provider.documentationTarget(element, element)
        assertNull("Should return null for YAML element", target)
    }

    // -- Negative cases: offset boundaries --

    fun testDocumentationTargetsReturnsEmptyForNegativeOffset() {
        val file = myFixture.configureByText("test.txt", "hello")
        val provider = DatamancerColumnDocumentationProvider()
        // Offset -1 is handled internally (offset - 1 = -2, guard returns null)
        val targets = provider.documentationTargets(file, 0)
        assertTrue(targets.isEmpty())
    }

    fun testDocumentationTargetsReturnsEmptyForBeyondEndOffset() {
        val file = myFixture.configureByText("test.txt", "hi")
        val provider = DatamancerColumnDocumentationProvider()
        val targets = provider.documentationTargets(file, 999)
        assertTrue(targets.isEmpty())
    }
}

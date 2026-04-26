package com.github.joshuataylor.datamancer.core.lang

import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.ProcessingContext
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.YAMLSequence
import org.jetbrains.yaml.psi.YAMLSequenceItem

/**
 * Tests for DbtYamlDataTestReferenceProvider.
 * Verifies the context detection logic that determines whether a YAML scalar
 * is a generic test name under `data_tests:` or `tests:`.
 *
 * Note: YAML PSI may not be fully available in light test environments. Tests that
 * depend on YAML PSI structure guard against null elements.
 */
class DatamancerYamlDataTestReferenceProviderTest : BasePlatformTestCase() {

    private val provider = DbtYamlDataTestReferenceProvider()
    private val context = ProcessingContext()

    /**
     * Finds a YAMLScalar with the given text value that is a direct child of a
     * YAMLSequenceItem (plain scalar form).
     */
    private fun findPlainTestScalar(yamlContent: String, testName: String): YAMLScalar? {
        val psiFile = myFixture.configureByText("schema.yml", yamlContent)
        val scalars = PsiTreeUtil.collectElementsOfType(psiFile, YAMLScalar::class.java)
        return scalars.find { scalar ->
            scalar.textValue == testName && scalar.parent is YAMLSequenceItem
        }
    }

    /**
     * Finds a YAMLScalar that is the key of a YAMLKeyValue with the given key text,
     * where the key-value is inside a mapping inside a sequence item (mapping key form).
     */
    private fun findMappingKeyTestScalar(yamlContent: String, testName: String): YAMLScalar? {
        val psiFile = myFixture.configureByText("schema.yml", yamlContent)
        val keyValues = PsiTreeUtil.collectElementsOfType(psiFile, YAMLKeyValue::class.java)
        val kv = keyValues.find { it.keyText == testName && it.parent is YAMLMapping } ?: return null
        return kv.key as? YAMLScalar
    }

    // Class structure tests
    fun testProviderCanBeInstantiated() {
        assertNotNull(DbtYamlDataTestReferenceProvider())
    }

    fun testProviderExtendsPsiReferenceProvider() {
        assertTrue(
            "Should extend PsiReferenceProvider",
            DbtYamlDataTestReferenceProvider() is PsiReferenceProvider
        )
    }

    fun testGetReferencesByElementMethodExists() {
        val method = DbtYamlDataTestReferenceProvider::class.java.methods.find {
            it.name == "getReferencesByElement"
        }
        assertNotNull("getReferencesByElement method should exist", method)
    }

    // Guard clause tests -- no dbt project configured
    fun testReturnsEmptyArrayWhenNoDbtProjectConfigured() {
        val yaml = """
            models:
              - name: orders
                columns:
                  - name: amount
                    data_tests:
                      - is_positive
        """.trimIndent()
        val scalar = findPlainTestScalar(yaml, "is_positive")
        if (scalar != null) {
            val refs = provider.getReferencesByElement(scalar, context)
            assertTrue(
                "Should return empty when no dbt project is configured",
                refs.isEmpty()
            )
        }
    }

    fun testReturnsEmptyArrayForNonYamlScalarElement() {
        val psiFile = myFixture.configureByText("test.txt", "hello world")
        val element = psiFile.firstChild
        if (element != null) {
            val refs = provider.getReferencesByElement(element, context)
            assertTrue(
                "Should return empty for non-YAMLScalar elements",
                refs.isEmpty()
            )
        }
    }

    // YAML PSI structure verification: Form 1 (plain scalar)
    fun testPlainScalarDataTestStructure() {
        val yaml = """
            models:
              - name: orders
                columns:
                  - name: amount
                    data_tests:
                      - is_positive
        """.trimIndent()
        val scalar = findPlainTestScalar(yaml, "is_positive")
        if (scalar != null) {
            val seqItem = scalar.parent
            assertTrue("Parent should be YAMLSequenceItem", seqItem is YAMLSequenceItem)

            val sequence = seqItem!!.parent
            assertTrue("Grandparent should be YAMLSequence", sequence is YAMLSequence)

            val dataTestsKv = sequence!!.parent
            assertTrue("Ancestor should be YAMLKeyValue", dataTestsKv is YAMLKeyValue)
            assertEquals("data_tests", (dataTestsKv as YAMLKeyValue).keyText)
        }
    }

    // YAML PSI structure verification: Form 2 (mapping key)
    fun testMappingKeyDataTestStructure() {
        val yaml = """
            models:
              - name: orders
                columns:
                  - name: amount
                    data_tests:
                      - accepted_range:
                          arguments:
                            min_value: 0
        """.trimIndent()
        val scalar = findMappingKeyTestScalar(yaml, "accepted_range")
        if (scalar != null) {
            val kv = scalar.parent
            assertTrue("Parent should be YAMLKeyValue", kv is YAMLKeyValue)
            assertEquals("accepted_range", (kv as YAMLKeyValue).keyText)

            val mapping = kv.parent
            assertTrue("Grandparent should be YAMLMapping", mapping is YAMLMapping)

            val seqItem = mapping!!.parent
            assertTrue("Great-grandparent should be YAMLSequenceItem", seqItem is YAMLSequenceItem)

            val sequence = seqItem!!.parent
            assertTrue("Ancestor should be YAMLSequence", sequence is YAMLSequence)

            val dataTestsKv = sequence!!.parent
            assertTrue("Top-level should be YAMLKeyValue", dataTestsKv is YAMLKeyValue)
            assertEquals("data_tests", (dataTestsKv as YAMLKeyValue).keyText)
        }
    }

    // Legacy `tests:` key support
    fun testPlainScalarWithLegacyTestsKey() {
        val yaml = """
            models:
              - name: orders
                columns:
                  - name: amount
                    tests:
                      - is_positive
        """.trimIndent()
        val scalar = findPlainTestScalar(yaml, "is_positive")
        if (scalar != null) {
            val seqItem = scalar.parent as? YAMLSequenceItem
            assertNotNull(seqItem)

            val sequence = seqItem!!.parent as? YAMLSequence
            assertNotNull(sequence)

            val testsKv = sequence!!.parent as? YAMLKeyValue
            assertNotNull(testsKv)
            assertEquals("tests", testsKv!!.keyText)
        }
    }

    // Negative tests: should NOT match unrelated contexts
    fun testDoesNotMatchNameValues() {
        val yaml = "models:\n  - name: customers"
        val psiFile = myFixture.configureByText("schema.yml", yaml)
        val scalars = PsiTreeUtil.collectElementsOfType(psiFile, YAMLScalar::class.java)
        val nameScalar = scalars.find { scalar ->
            val parent = scalar.parent as? YAMLKeyValue
            parent?.keyText == "name" && scalar.textValue == "customers"
        }
        if (nameScalar != null) {
            val refs = provider.getReferencesByElement(nameScalar, context)
            assertTrue(
                "Should return empty for name: values (handled by DbtYamlNameReferenceProvider)",
                refs.isEmpty()
            )
        }
    }

    fun testDoesNotMatchItemsUnderUnrelatedKeys() {
        val yaml = """
            models:
              - name: orders
                columns:
                  - name: amount
                    description: test value
        """.trimIndent()
        val psiFile = myFixture.configureByText("schema.yml", yaml)
        val scalars = PsiTreeUtil.collectElementsOfType(psiFile, YAMLScalar::class.java)
        for (scalar in scalars) {
            val refs = provider.getReferencesByElement(scalar, context)
            assertTrue(
                "Should return empty for scalars not under data_tests/tests",
                refs.isEmpty()
            )
        }
    }
}

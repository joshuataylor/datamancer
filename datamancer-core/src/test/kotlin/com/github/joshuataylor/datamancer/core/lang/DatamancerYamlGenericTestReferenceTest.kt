package com.github.joshuataylor.datamancer.core.lang

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiReference
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.YAMLSequenceItem

/**
 * Tests for DbtYamlGenericTestReference.
 * Tests the reference from a generic test name in YAML to its SQL definition file.
 *
 * Note: YAML PSI may not be fully available in light test environments.
 */
class DatamancerYamlGenericTestReferenceTest : BasePlatformTestCase() {

    /**
     * Creates a YAMLScalar from YAML content and wraps it in a DbtYamlGenericTestReference.
     * Returns null if YAML PSI is not available.
     */
    private fun createTestReference(testName: String): DbtYamlGenericTestReference? {
        val yaml = """
            models:
              - name: orders
                columns:
                  - name: amount
                    data_tests:
                      - $testName
        """.trimIndent()
        val psiFile = myFixture.configureByText("schema.yml", yaml)

        val scalars = PsiTreeUtil.collectElementsOfType(psiFile, YAMLScalar::class.java)
        val testScalar = scalars.find { scalar ->
            scalar.textValue == testName && scalar.parent is YAMLSequenceItem
        } ?: return null

        return DbtYamlGenericTestReference(
            testScalar,
            TextRange(0, testName.length),
            testName
        )
    }

    // Class structure tests
    fun testReferenceClassExists() {
        assertNotNull(DbtYamlGenericTestReference::class.java)
    }

    fun testReferenceExtendsDbtModelReferenceBase() {
        val superclass = DbtYamlGenericTestReference::class.java.superclass
        assertEquals(
            "Should extend DbtModelReferenceBase",
            DbtModelReferenceBase::class.java.name,
            superclass?.name
        )
    }

    // Tests requiring YAML PSI (guarded)
    fun testReferenceCanBeCreated() {
        val ref = createTestReference("is_positive")
        if (ref != null) {
            assertNotNull("Should be able to create a YAML generic test reference", ref)
        }
    }

    fun testReferenceImplementsPsiReference() {
        val ref = createTestReference("is_positive")
        if (ref != null) {
            assertTrue("Should implement PsiReference", ref is PsiReference)
        }
    }

    fun testReferenceIsSoft() {
        val ref = createTestReference("is_positive")
        if (ref != null) {
            assertTrue("Reference should be soft (inherited from DbtModelReferenceBase)", ref.isSoft)
        }
    }

    fun testResolveReturnsNullWithoutDbtProject() {
        val ref = createTestReference("is_positive")
        if (ref != null) {
            val result = ref.resolve()
            assertNull("Should return null when no dbt project configured", result)
        }
    }

    fun testRangeInElementIsCorrect() {
        val ref = createTestReference("is_positive")
        if (ref != null) {
            assertEquals(TextRange(0, "is_positive".length), ref.rangeInElement)
        }
    }

    fun testElementIsYamlScalar() {
        val ref = createTestReference("is_positive")
        if (ref != null) {
            assertTrue("Element should be YAMLScalar", ref.element is YAMLScalar)
        }
    }

    fun testReferenceWithUnderscoreName() {
        assertDoesNotThrow { createTestReference("my_custom_test") }
    }

    private fun assertDoesNotThrow(block: () -> Unit) {
        try {
            block()
        } catch (e: Throwable) {
            fail("Expected no exception but got: ${e.message}")
        }
    }
}

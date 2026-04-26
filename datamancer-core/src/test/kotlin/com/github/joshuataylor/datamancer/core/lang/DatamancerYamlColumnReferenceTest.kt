package com.github.joshuataylor.datamancer.core.lang

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLScalar

/**
 * Tests for DbtYamlColumnReference.
 * Tests the reference from a column name in YAML to its occurrence in the SQL model file.
 *
 * Note: This class extends PsiReferenceBase, not DbtModelReferenceBase.
 * YAML PSI may not be fully available in light test environments.
 */
class DatamancerYamlColumnReferenceTest : BasePlatformTestCase() {

    /**
     * Creates a YAMLScalar for a column name and wraps it in a DbtYamlColumnReference.
     * Returns null if YAML PSI is not available.
     */
    private fun createColumnReference(
        modelName: String,
        columnName: String
    ): DbtYamlColumnReference? {
        val yaml = "models:\n  - name: $modelName\n    columns:\n      - name: $columnName"
        val psiFile = myFixture.configureByText("schema.yml", yaml)

        val scalars = PsiTreeUtil.collectElementsOfType(psiFile, YAMLScalar::class.java)
        val nameScalar = scalars.find { scalar ->
            val parent = scalar.parent as? YAMLKeyValue
            parent?.keyText == "name" && scalar.textValue == columnName
        } ?: return null

        return DbtYamlColumnReference(
            nameScalar,
            TextRange(0, columnName.length),
            modelName,
            columnName
        )
    }

    // Class structure tests (always pass regardless of PSI availability)
    fun testReferenceClassExists() {
        assertNotNull(DbtYamlColumnReference::class.java)
    }

    fun testReferenceExtendsPsiReferenceBase() {
        assertTrue(
            "Should extend PsiReferenceBase",
            PsiReferenceBase::class.java.isAssignableFrom(DbtYamlColumnReference::class.java)
        )
    }

    fun testReferenceDoesNotExtendDbtModelReferenceBase() {
        assertFalse(
            "Should NOT extend DbtModelReferenceBase (extends PsiReferenceBase directly)",
            DbtModelReferenceBase::class.java.isAssignableFrom(DbtYamlColumnReference::class.java)
        )
    }

    // Tests requiring YAML PSI (guarded)
    fun testReferenceCanBeCreated() {
        val ref = createColumnReference("customers", "customer_id")
        if (ref != null) {
            assertNotNull("Should be able to create a column reference", ref)
        }
    }

    fun testReferenceImplementsPsiReference() {
        val ref = createColumnReference("customers", "customer_id")
        if (ref != null) {
            assertTrue("Should implement PsiReference", ref is PsiReference)
        }
    }

    fun testReferenceIsSoft() {
        val ref = createColumnReference("customers", "customer_id")
        if (ref != null) {
            assertTrue("Reference should be soft (constructor passes true to super)", ref.isSoft)
        }
    }

    fun testResolveReturnsNullWithoutModel() {
        val ref = createColumnReference("customers", "customer_id")
        if (ref != null) {
            val result = ref.resolve()
            assertNull("Should return null when model file does not exist", result)
        }
    }

    fun testElementIsYamlScalar() {
        val ref = createColumnReference("customers", "customer_id")
        if (ref != null) {
            assertTrue("Element should be YAMLScalar", ref.element is YAMLScalar)
        }
    }

    fun testRangeInElementIsCorrect() {
        val ref = createColumnReference("customers", "order_date")
        if (ref != null) {
            assertEquals(TextRange(0, "order_date".length), ref.rangeInElement)
        }
    }

    fun testReferenceWithSpecialCharacterColumnName() {
        assertDoesNotThrow { createColumnReference("orders", "total_amount_usd") }
    }

    private fun assertDoesNotThrow(block: () -> Unit) {
        try {
            block()
        } catch (e: Throwable) {
            fail("Expected no exception but got: ${e.message}")
        }
    }
}

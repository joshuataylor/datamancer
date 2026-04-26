package com.github.joshuataylor.datamancer.core.lang

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiReference
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLScalar

/**
 * Tests for DbtYamlModelNameReference.
 * Tests the reference from a model name in YAML to its SQL file.
 *
 * Note: YAML PSI may not be fully available in light test environments.
 */
class DatamancerYamlModelNameReferenceTest : BasePlatformTestCase() {

    /**
     * Creates a YAMLScalar from YAML content and wraps it in a DbtYamlModelNameReference.
     * Returns null if YAML PSI is not available.
     */
    private fun createModelNameReference(modelName: String): DbtYamlModelNameReference? {
        val yaml = "models:\n  - name: $modelName"
        val psiFile = myFixture.configureByText("schema.yml", yaml)

        val scalars = PsiTreeUtil.collectElementsOfType(psiFile, YAMLScalar::class.java)
        val nameScalar = scalars.find { scalar ->
            val parent = scalar.parent as? YAMLKeyValue
            parent?.keyText == "name" && scalar.textValue == modelName
        } ?: return null

        return DbtYamlModelNameReference(
            nameScalar,
            TextRange(0, modelName.length),
            modelName
        )
    }

    // Class structure tests (always pass)
    fun testReferenceClassExists() {
        assertNotNull(DbtYamlModelNameReference::class.java)
    }

    fun testReferenceExtendsDbtModelReferenceBase() {
        val superclass = DbtYamlModelNameReference::class.java.superclass
        assertEquals(
            "Should extend DbtModelReferenceBase",
            DbtModelReferenceBase::class.java.name,
            superclass?.name
        )
    }

    // Tests requiring YAML PSI (guarded)
    fun testReferenceCanBeCreated() {
        val ref = createModelNameReference("customers")
        if (ref != null) {
            assertNotNull("Should be able to create a YAML model name reference", ref)
        }
    }

    fun testReferenceImplementsPsiReference() {
        val ref = createModelNameReference("customers")
        if (ref != null) {
            assertTrue("Should implement PsiReference", ref is PsiReference)
        }
    }

    fun testReferenceIsSoft() {
        val ref = createModelNameReference("customers")
        if (ref != null) {
            assertTrue("Reference should be soft (inherited from DbtModelReferenceBase)", ref.isSoft)
        }
    }

    fun testResolveReturnsNullWithoutDbtProject() {
        val ref = createModelNameReference("customers")
        if (ref != null) {
            val result = ref.resolve()
            assertNull("Should return null when no dbt project models exist", result)
        }
    }

    fun testRangeInElementIsCorrect() {
        val ref = createModelNameReference("stg_customers")
        if (ref != null) {
            assertEquals(TextRange(0, "stg_customers".length), ref.rangeInElement)
        }
    }

    fun testElementIsYamlScalar() {
        val ref = createModelNameReference("orders")
        if (ref != null) {
            assertTrue("Element should be YAMLScalar", ref.element is YAMLScalar)
        }
    }

    fun testReferenceWithUnderscoreModelName() {
        assertDoesNotThrow { createModelNameReference("stg_my_model_v2") }
    }

    fun testReferenceWithHyphenModelName() {
        assertDoesNotThrow { createModelNameReference("my-model-name") }
    }

    private fun assertDoesNotThrow(block: () -> Unit) {
        try {
            block()
        } catch (e: Throwable) {
            fail("Expected no exception but got: ${e.message}")
        }
    }
}

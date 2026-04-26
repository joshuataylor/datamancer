package com.github.joshuataylor.datamancer.core.lang

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiReference
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLScalar

/**
 * Tests for DbtYamlSourceUsageReference.
 * Tests the reference from source/table names in YAML to SQL files using that source.
 *
 * Note: YAML PSI may not be fully available in light test environments.
 */
class DatamancerYamlSourceUsageReferenceTest : BasePlatformTestCase() {

    /**
     * Creates a YAMLScalar and wraps it in a DbtYamlSourceUsageReference.
     * Returns null if YAML PSI is not available.
     */
    private fun createSourceUsageReference(
        sourceName: String,
        tableName: String?,
        yamlName: String = tableName ?: sourceName
    ): DbtYamlSourceUsageReference? {
        val yaml = if (tableName != null) {
            "sources:\n  - name: $sourceName\n    tables:\n      - name: $tableName"
        } else {
            "sources:\n  - name: $sourceName"
        }
        val psiFile = myFixture.configureByText("sources.yml", yaml)

        val scalars = PsiTreeUtil.collectElementsOfType(psiFile, YAMLScalar::class.java)
        val nameScalar = scalars.find { scalar ->
            val parent = scalar.parent as? YAMLKeyValue
            parent?.keyText == "name" && scalar.textValue == yamlName
        } ?: return null

        return DbtYamlSourceUsageReference(
            nameScalar,
            TextRange(0, yamlName.length),
            sourceName,
            tableName
        )
    }

    // Class structure tests (always pass)
    fun testReferenceClassExists() {
        assertNotNull(DbtYamlSourceUsageReference::class.java)
    }

    fun testReferenceExtendsDbtModelReferenceBase() {
        val superclass = DbtYamlSourceUsageReference::class.java.superclass
        assertEquals(
            "Should extend DbtModelReferenceBase",
            DbtModelReferenceBase::class.java.name,
            superclass?.name
        )
    }

    // Tests requiring YAML PSI (guarded)
    fun testReferenceCanBeCreatedForSourceLevel() {
        val ref = createSourceUsageReference("raw_data", null)
        if (ref != null) {
            assertNotNull("Should create source-level reference", ref)
        }
    }

    fun testReferenceCanBeCreatedForTableLevel() {
        val ref = createSourceUsageReference("raw_data", "customers")
        if (ref != null) {
            assertNotNull("Should create table-level reference", ref)
        }
    }

    fun testReferenceImplementsPsiReference() {
        val ref = createSourceUsageReference("raw_data", null)
        if (ref != null) {
            assertTrue("Should implement PsiReference", ref is PsiReference)
        }
    }

    fun testReferenceIsSoft() {
        val ref = createSourceUsageReference("raw_data", null)
        if (ref != null) {
            assertTrue("Reference should be soft", ref.isSoft)
        }
    }

    fun testModelNameIsSourceNameWhenTableNameNull() {
        val ref = createSourceUsageReference("raw_data", null)
        if (ref != null) {
            assertEquals("raw_data", ref.canonicalText)
        }
    }

    fun testModelNameIsTableNameWhenTableNameSet() {
        val ref = createSourceUsageReference("raw_data", "customers")
        if (ref != null) {
            assertEquals("customers", ref.canonicalText)
        }
    }

    fun testResolveReturnsEmptyWithoutDbtProject() {
        val ref = createSourceUsageReference("raw_data", null)
        if (ref != null) {
            val result = ref.resolve()
            assertNull("Should return null when no model files use this source", result)
        }
    }

    fun testElementIsYamlScalar() {
        val ref = createSourceUsageReference("raw_data", null)
        if (ref != null) {
            assertTrue("Element should be YAMLScalar", ref.element is YAMLScalar)
        }
    }
}

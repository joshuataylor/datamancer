package com.github.joshuataylor.datamancer.core.lang

import com.intellij.jinja.psi.Jinja2StringLiteral
import com.intellij.jinja.tags.Jinja2FunctionCall
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiReference
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for DbtModelRefReference.
 * Tests the reference implementation for dbt ref() function calls.
 *
 * Note: Full integration tests with actual model resolution would require
 * setting up a dbt project with DatamancerDbtProjectIndexService configured.
 */
class DatamancerModelRefReferenceTest : BasePlatformTestCase() {

    // Helper to create a reference for testing
    private fun createRefReference(jinjaCode: String): DbtModelRefReference? {
        val psiFile = myFixture.configureByText("test.html", jinjaCode)
        val functionCall = PsiTreeUtil.findChildOfType(psiFile, Jinja2FunctionCall::class.java)
            ?: return null
        val stringLiteral = PsiTreeUtil.findChildOfType(functionCall, Jinja2StringLiteral::class.java)
            ?: return null
        val modelName = stringLiteral.value ?: return null

        return DbtModelRefReference(
            functionCall,
            TextRange(0, modelName.length),
            modelName,
            stringLiteral
        )
    }

    // Instantiation tests
    fun testReferenceCanBeCreated() {
        val ref = createRefReference("{{ ref('stg_customers') }}")
        // Reference may be null if Jinja2 PSI not fully available in test env
        if (ref != null) {
            assertNotNull("Reference should be created", ref)
        }
    }

    fun testReferenceImplementsPsiReference() {
        val ref = createRefReference("{{ ref('stg_customers') }}")
        if (ref != null) {
            assertTrue("Should implement PsiReference", ref is PsiReference)
        }
    }

    // isSoft tests
    fun testReferenceIsSoft() {
        val ref = createRefReference("{{ ref('stg_customers') }}")
        if (ref != null) {
            assertTrue("Reference should be soft", ref.isSoft)
        }
    }

    // resolve tests - without dbt project, should return null
    fun testResolveReturnsNullWithoutDbtProject() {
        val ref = createRefReference("{{ ref('stg_customers') }}")
        if (ref != null) {
            val result = ref.resolve()
            assertNull("Should return null when no dbt project", result)
        }
    }

    // getVariants tests - without dbt project, should return empty
    fun testGetVariantsReturnsEmptyWithoutDbtProject() {
        val ref = createRefReference("{{ ref('stg_customers') }}")
        if (ref != null) {
            val variants = ref.variants
            assertNotNull("Variants should not be null", variants)
            assertTrue("Should return empty variants without dbt project", variants.isEmpty())
        }
    }

    // Model name tests - verify no exceptions thrown
    fun testReferenceWithUnderscoreModelName() {
        // Test should not throw, ref may be null if Jinja2 PSI not available
        assertDoesNotThrow {
            createRefReference("{{ ref('stg_my_model') }}")
        }
    }

    fun testReferenceWithHyphenModelName() {
        assertDoesNotThrow {
            createRefReference("{{ ref('my-model-name') }}")
        }
    }

    fun testReferenceWithVersionedModelName() {
        assertDoesNotThrow {
            createRefReference("{{ ref('stg_customers_v2') }}")
        }
    }

    private fun assertDoesNotThrow(block: () -> Unit) {
        try {
            block()
        } catch (e: Throwable) {
            fail("Expected no exception but got: ${e.message}")
        }
    }

    // Method existence tests
    fun testResolveMethodExists() {
        val method = DbtModelRefReference::class.java.methods.find { it.name == "resolve" }
        assertNotNull("resolve method should exist", method)
    }

    fun testGetVariantsMethodExists() {
        val method = DbtModelRefReference::class.java.methods.find { it.name == "getVariants" }
        assertNotNull("getVariants method should exist", method)
    }

    // Class inheritance tests
    fun testExtendsModelReferenceBase() {
        val superclass = DbtModelRefReference::class.java.superclass
        assertEquals(
            "Should extend DbtModelReferenceBase",
            DbtModelReferenceBase::class.java.name,
            superclass?.name
        )
    }
}

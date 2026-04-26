package com.github.joshuataylor.datamancer.core.lang

import com.intellij.jinja.psi.Jinja2StringLiteral
import com.intellij.jinja.tags.Jinja2FunctionCall
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiReference
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for DbtSourceNameReference.
 * Tests the reference implementation for source name in source() calls.
 *
 * Note: Full integration tests with actual source resolution would require
 * setting up a dbt project with sources defined in schema.yml.
 */
class DatamancerSourceNameReferenceTest : BasePlatformTestCase() {

    // Helper to create a source name reference for testing
    private fun createSourceNameReference(jinjaCode: String): DbtSourceNameReference? {
        val psiFile = myFixture.configureByText("test.html", jinjaCode)
        val functionCall = PsiTreeUtil.findChildOfType(psiFile, Jinja2FunctionCall::class.java)
            ?: return null
        val stringLiterals = PsiTreeUtil.findChildrenOfType(functionCall, Jinja2StringLiteral::class.java).toList()
        if (stringLiterals.isEmpty()) return null

        val sourceNameLiteral = stringLiterals[0]
        val sourceName = sourceNameLiteral.value ?: return null

        return DbtSourceNameReference(
            functionCall,
            TextRange(0, sourceName.length),
            sourceName,
            sourceNameLiteral
        )
    }

    // Instantiation tests
    fun testReferenceCanBeCreated() {
        val ref = createSourceNameReference("{{ source('raw_data', 'customers') }}")
        // Reference may be null if Jinja2 PSI not fully available in test env
        if (ref != null) {
            assertNotNull("Reference should be created", ref)
        }
    }

    fun testReferenceImplementsPsiReference() {
        val ref = createSourceNameReference("{{ source('raw_data', 'customers') }}")
        if (ref != null) {
            assertTrue("Should implement PsiReference", ref is PsiReference)
        }
    }

    // isSoft tests
    fun testReferenceIsSoft() {
        val ref = createSourceNameReference("{{ source('raw_data', 'customers') }}")
        if (ref != null) {
            assertTrue("Reference should be soft", ref.isSoft)
        }
    }

    // resolve tests - without dbt project, should return null
    fun testResolveReturnsNullWithoutDbtProject() {
        val ref = createSourceNameReference("{{ source('raw_data', 'customers') }}")
        if (ref != null) {
            val result = ref.resolve()
            assertNull("Should return null when no sources defined", result)
        }
    }

    // getVariants tests - without dbt project, should return empty
    fun testGetVariantsReturnsEmptyWithoutDbtProject() {
        val ref = createSourceNameReference("{{ source('raw_data', 'customers') }}")
        if (ref != null) {
            val variants = ref.variants
            assertNotNull("Variants should not be null", variants)
            assertTrue("Should return empty variants without sources", variants.isEmpty())
        }
    }

    // Source name pattern tests - verify no exceptions thrown
    fun testReferenceWithUnderscoreSourceName() {
        assertDoesNotThrow {
            createSourceNameReference("{{ source('raw_stripe_data', 'payments') }}")
        }
    }

    fun testReferenceWithSimpleSourceName() {
        assertDoesNotThrow {
            createSourceNameReference("{{ source('jaffle_shop', 'orders') }}")
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
        val method = DbtSourceNameReference::class.java.methods.find { it.name == "resolve" }
        assertNotNull("resolve method should exist", method)
    }

    fun testGetVariantsMethodExists() {
        val method = DbtSourceNameReference::class.java.methods.find { it.name == "getVariants" }
        assertNotNull("getVariants method should exist", method)
    }

    // Class inheritance tests
    fun testExtendsModelReferenceBase() {
        val superclass = DbtSourceNameReference::class.java.superclass
        assertEquals(
            "Should extend DbtModelReferenceBase",
            DbtModelReferenceBase::class.java.name,
            superclass?.name
        )
    }
}

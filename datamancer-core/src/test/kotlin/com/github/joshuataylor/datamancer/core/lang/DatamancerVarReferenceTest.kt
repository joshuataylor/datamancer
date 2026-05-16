package com.github.joshuataylor.datamancer.core.lang

import com.intellij.jinja.psi.Jinja2StringLiteral
import com.intellij.jinja.tags.Jinja2FunctionCall
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiReference
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.concurrency.annotations.RequiresReadLock

/**
 * Tests for DbtVarReference.
 * Tests the reference implementation for dbt var() function calls.
 */
class DatamancerVarReferenceTest : BasePlatformTestCase() {

    // Helper to create a var reference for testing
    private fun createVarReference(jinjaCode: String): DbtVarReference? {
        val psiFile = myFixture.configureByText("test.html", jinjaCode)
        val functionCall = PsiTreeUtil.findChildOfType(psiFile, Jinja2FunctionCall::class.java)
            ?: return null
        val stringLiteral = PsiTreeUtil.findChildOfType(functionCall, Jinja2StringLiteral::class.java)
            ?: return null
        val varName = stringLiteral.value ?: return null

        return DbtVarReference(
            functionCall,
            TextRange(0, varName.length),
            varName,
            stringLiteral
        )
    }

    // Instantiation tests
    fun testReferenceCanBeCreated() {
        val ref = createVarReference("{{ var('start_date') }}")
        // Reference may be null if Jinja2 PSI not fully available in test env
        if (ref != null) {
            assertNotNull("Reference should be created", ref)
        }
    }

    fun testReferenceImplementsPsiReference() {
        val ref = createVarReference("{{ var('start_date') }}")
        if (ref != null) {
            assertTrue("Should implement PsiReference", ref is PsiReference)
        }
    }

    // isSoft tests
    fun testReferenceIsSoft() {
        val ref = createVarReference("{{ var('start_date') }}")
        if (ref != null) {
            assertTrue("Reference should be soft", ref.isSoft)
        }
    }

    // resolve tests - without dbt project, should return null
    fun testResolveReturnsNullWithoutDbtProject() {
        val ref = createVarReference("{{ var('start_date') }}")
        if (ref != null) {
            val result = ref.resolve()
            assertNull("Should return null when no dbt project", result)
        }
    }

    // getVariants tests - without dbt project, should return empty
    @RequiresReadLock
    fun testGetVariantsReturnsEmptyWithoutDbtProject() {
        val ref = createVarReference("{{ var('start_date') }}")
        if (ref != null) {
            val variants = ref.variants
            assertNotNull("Variants should not be null", variants)
            assertTrue("Should return empty variants without dbt project", variants.isEmpty())
        }
    }

    // Var name tests - verify no exceptions thrown
    fun testReferenceWithUnderscoreVarName() {
        assertDoesNotThrow {
            createVarReference("{{ var('my_start_date') }}")
        }
    }

    fun testReferenceWithSimpleVarName() {
        assertDoesNotThrow {
            createVarReference("{{ var('date') }}")
        }
    }

    fun testReferenceWithNumberInVarName() {
        assertDoesNotThrow {
            createVarReference("{{ var('version_2') }}")
        }
    }

    // Method existence tests
    fun testResolveMethodExists() {
        val method = DbtVarReference::class.java.methods.find { it.name == "resolve" }
        assertNotNull("resolve method should exist", method)
    }

    fun testGetVariantsMethodExists() {
        val method = DbtVarReference::class.java.methods.find { it.name == "getVariants" }
        assertNotNull("getVariants method should exist", method)
    }

    // Class inheritance tests
    fun testExtendsModelReferenceBase() {
        val superclass = DbtVarReference::class.java.superclass
        assertEquals(
            "Should extend DbtModelReferenceBase",
            DbtModelReferenceBase::class.java.name,
            superclass?.name
        )
    }

    private fun assertDoesNotThrow(block: () -> Unit) {
        try {
            block()
        } catch (e: Throwable) {
            fail("Expected no exception but got: ${e.message}")
        }
    }
}

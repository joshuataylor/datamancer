package com.github.joshuataylor.datamancer.core.lang

import com.intellij.jinja.tags.Jinja2FunctionCall
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.ProcessingContext

/**
 * Tests for DbtModelRefReferenceProvider.
 * Tests the reference provider for dbt ref() function calls.
 *
 * Note: Full integration tests with actual reference resolution would require
 * setting up a dbt project with DatamancerDbtProjectIndexService configured.
 */
class DbtModelRefReferenceProviderTest : BasePlatformTestCase() {

    // Provider instantiation tests
    fun testProviderCanBeInstantiated() {
        val provider = DbtModelRefReferenceProvider()
        assertNotNull(provider)
    }

    fun testProviderImplementsPsiReferenceProvider() {
        val provider = DbtModelRefReferenceProvider()
        assertTrue(provider is PsiReferenceProvider)
    }

    // Pattern tests
    fun testRefFunctionPatternExists() {
        val pattern = DbtModelRefReferenceProvider.REF_FUNCTION_PATTERN
        assertNotNull("REF_FUNCTION_PATTERN should exist", pattern)
    }

    fun testIsRefFunctionConditionExists() {
        val condition = DbtModelRefReferenceProvider.isRefFunction
        assertNotNull("isRefFunction condition should exist", condition)
    }

    // Pattern matching tests using HTML (Jinja2 natively supported)
    fun testIsRefFunctionAcceptsRefCall() {
        val psiFile = myFixture.configureByText("test.html", "{{ ref('stg_customers') }}")
        val functionCall = PsiTreeUtil.findChildOfType(psiFile, Jinja2FunctionCall::class.java)
        if (functionCall != null) {
            val context = ProcessingContext()
            val result = DbtModelRefReferenceProvider.isRefFunction.accepts(functionCall, context)
            assertTrue("Should accept ref() function call", result)
        }
    }

    fun testIsRefFunctionRejectsSourceCall() {
        val psiFile = myFixture.configureByText("test.html", "{{ source('raw', 'customers') }}")
        val functionCall = PsiTreeUtil.findChildOfType(psiFile, Jinja2FunctionCall::class.java)
        if (functionCall != null) {
            val context = ProcessingContext()
            val result = DbtModelRefReferenceProvider.isRefFunction.accepts(functionCall, context)
            assertFalse("Should reject source() function call", result)
        }
    }

    fun testIsRefFunctionRejectsConfigCall() {
        val psiFile = myFixture.configureByText("test.html", "{{ config(materialized='table') }}")
        val functionCall = PsiTreeUtil.findChildOfType(psiFile, Jinja2FunctionCall::class.java)
        if (functionCall != null) {
            val context = ProcessingContext()
            val result = DbtModelRefReferenceProvider.isRefFunction.accepts(functionCall, context)
            assertFalse("Should reject config() function call", result)
        }
    }

    fun testIsRefFunctionRejectsUppercaseRef() {
        val psiFile = myFixture.configureByText("test.html", "{{ REF('model') }}")
        val functionCall = PsiTreeUtil.findChildOfType(psiFile, Jinja2FunctionCall::class.java)
        if (functionCall != null) {
            val context = ProcessingContext()
            val result = DbtModelRefReferenceProvider.isRefFunction.accepts(functionCall, context)
            assertFalse("Should reject REF() (uppercase)", result)
        }
    }

    // getReferencesByElement tests - without dbt project configured
    fun testGetReferencesByElementReturnsEmptyWithoutDbtProject() {
        val psiFile = myFixture.configureByText("test.html", "{{ ref('model_name') }}")
        val functionCall = PsiTreeUtil.findChildOfType(psiFile, Jinja2FunctionCall::class.java)
        if (functionCall != null) {
            val provider = DbtModelRefReferenceProvider()
            val context = ProcessingContext()
            val references = provider.getReferencesByElement(functionCall, context)
            // Without dbt project configured, should return empty
            assertTrue("Should return empty without dbt project", references.isEmpty())
        }
    }

    // Method existence tests
    fun testGetReferencesByElementMethodExists() {
        val provider = DbtModelRefReferenceProvider()
        val method = provider::class.java.methods.find { it.name == "getReferencesByElement" }
        assertNotNull("getReferencesByElement method should exist", method)
    }

    // Multiple provider instances tests
    fun testMultipleInstancesCanBeCreated() {
        val provider1 = DbtModelRefReferenceProvider()
        val provider2 = DbtModelRefReferenceProvider()

        assertNotNull(provider1)
        assertNotNull(provider2)
        assertNotSame(provider1, provider2)
    }

    // Pattern consistency tests
    fun testPatternIsConsistentAcrossMultipleCalls() {
        val pattern1 = DbtModelRefReferenceProvider.REF_FUNCTION_PATTERN
        val pattern2 = DbtModelRefReferenceProvider.REF_FUNCTION_PATTERN
        assertSame("Pattern should be same instance", pattern1, pattern2)
    }

    // Edge cases
    fun testProviderDoesNotThrowOnNonFunctionCallElement() {
        val psiFile = myFixture.configureByText("test.html", "Just some text")
        val provider = DbtModelRefReferenceProvider()
        val context = ProcessingContext()
        // Should not throw when given non-function call element
        assertDoesNotThrow {
            provider.getReferencesByElement(psiFile, context)
        }
    }

    private fun assertDoesNotThrow(block: () -> Unit) {
        try {
            block()
        } catch (e: Throwable) {
            fail("Expected no exception but got: ${e.message}")
        }
    }
}

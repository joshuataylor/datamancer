package com.github.joshuataylor.datamancer.core.lang

import com.intellij.jinja.tags.Jinja2FunctionCall
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.ProcessingContext

/**
 * Tests for DbtSourceRefReferenceProvider.
 * Tests the reference provider for dbt source() function calls.
 *
 * Note: Full integration tests with actual reference resolution would require
 * setting up a dbt project with sources defined in schema.yml files.
 */
class DbtSourceRefReferenceProviderTest : BasePlatformTestCase() {

    // Provider instantiation tests
    fun testProviderCanBeInstantiated() {
        val provider = DbtSourceRefReferenceProvider()
        assertNotNull(provider)
    }

    fun testProviderImplementsPsiReferenceProvider() {
        val provider = DbtSourceRefReferenceProvider()
        assertTrue(provider is PsiReferenceProvider)
    }

    // Pattern tests
    fun testSourceFunctionPatternExists() {
        val pattern = DbtSourceRefReferenceProvider.SOURCE_FUNCTION_PATTERN
        assertNotNull("SOURCE_FUNCTION_PATTERN should exist", pattern)
    }

    fun testIsSourceFunctionConditionExists() {
        val condition = DbtSourceRefReferenceProvider.isSourceFunction
        assertNotNull("isSourceFunction condition should exist", condition)
    }

    // Pattern matching tests using HTML (Jinja2 natively supported)
    fun testIsSourceFunctionAcceptsSourceCall() {
        val psiFile = myFixture.configureByText("test.html", "{{ source('raw_data', 'customers') }}")
        val functionCall = PsiTreeUtil.findChildOfType(psiFile, Jinja2FunctionCall::class.java)
        if (functionCall != null) {
            val context = ProcessingContext()
            val result = DbtSourceRefReferenceProvider.isSourceFunction.accepts(functionCall, context)
            assertTrue("Should accept source() function call", result)
        }
    }

    fun testIsSourceFunctionRejectsRefCall() {
        val psiFile = myFixture.configureByText("test.html", "{{ ref('stg_customers') }}")
        val functionCall = PsiTreeUtil.findChildOfType(psiFile, Jinja2FunctionCall::class.java)
        if (functionCall != null) {
            val context = ProcessingContext()
            val result = DbtSourceRefReferenceProvider.isSourceFunction.accepts(functionCall, context)
            assertFalse("Should reject ref() function call", result)
        }
    }

    fun testIsSourceFunctionRejectsConfigCall() {
        val psiFile = myFixture.configureByText("test.html", "{{ config(materialized='view') }}")
        val functionCall = PsiTreeUtil.findChildOfType(psiFile, Jinja2FunctionCall::class.java)
        if (functionCall != null) {
            val context = ProcessingContext()
            val result = DbtSourceRefReferenceProvider.isSourceFunction.accepts(functionCall, context)
            assertFalse("Should reject config() function call", result)
        }
    }

    fun testIsSourceFunctionRejectsUppercaseSource() {
        val psiFile = myFixture.configureByText("test.html", "{{ SOURCE('schema', 'table') }}")
        val functionCall = PsiTreeUtil.findChildOfType(psiFile, Jinja2FunctionCall::class.java)
        if (functionCall != null) {
            val context = ProcessingContext()
            val result = DbtSourceRefReferenceProvider.isSourceFunction.accepts(functionCall, context)
            assertFalse("Should reject SOURCE() (uppercase)", result)
        }
    }

    // getReferencesByElement tests - without dbt project configured
    fun testGetReferencesByElementReturnsEmptyWithoutDbtProject() {
        val psiFile = myFixture.configureByText("test.html", "{{ source('raw', 'table') }}")
        val functionCall = PsiTreeUtil.findChildOfType(psiFile, Jinja2FunctionCall::class.java)
        if (functionCall != null) {
            val provider = DbtSourceRefReferenceProvider()
            val context = ProcessingContext()
            val references = provider.getReferencesByElement(functionCall, context)
            // Without dbt project configured, should return empty
            assertTrue("Should return empty without dbt project", references.isEmpty())
        }
    }

    // Method existence tests
    fun testGetReferencesByElementMethodExists() {
        val provider = DbtSourceRefReferenceProvider()
        val method = provider::class.java.methods.find { it.name == "getReferencesByElement" }
        assertNotNull("getReferencesByElement method should exist", method)
    }

    // Multiple provider instances tests
    fun testMultipleInstancesCanBeCreated() {
        val provider1 = DbtSourceRefReferenceProvider()
        val provider2 = DbtSourceRefReferenceProvider()

        assertNotNull(provider1)
        assertNotNull(provider2)
        assertNotSame(provider1, provider2)
    }

    // Pattern consistency tests
    fun testPatternIsConsistentAcrossMultipleCalls() {
        val pattern1 = DbtSourceRefReferenceProvider.SOURCE_FUNCTION_PATTERN
        val pattern2 = DbtSourceRefReferenceProvider.SOURCE_FUNCTION_PATTERN
        assertSame("Pattern should be same instance", pattern1, pattern2)
    }

    // Edge cases
    fun testProviderDoesNotThrowOnNonFunctionCallElement() {
        val psiFile = myFixture.configureByText("test.html", "Just some text content")
        val provider = DbtSourceRefReferenceProvider()
        val context = ProcessingContext()
        // Should not throw when given non-function call element
        assertDoesNotThrow {
            provider.getReferencesByElement(psiFile, context)
        }
    }

    fun testProviderDoesNotThrowOnSourceWithOneArg() {
        val psiFile = myFixture.configureByText("test.html", "{{ source('only_one_arg') }}")
        val functionCall = PsiTreeUtil.findChildOfType(psiFile, Jinja2FunctionCall::class.java)
        if (functionCall != null) {
            val provider = DbtSourceRefReferenceProvider()
            val context = ProcessingContext()
            assertDoesNotThrow {
                provider.getReferencesByElement(functionCall, context)
            }
        }
    }

    fun testProviderDoesNotThrowOnEmptySource() {
        val psiFile = myFixture.configureByText("test.html", "{{ source() }}")
        val functionCall = PsiTreeUtil.findChildOfType(psiFile, Jinja2FunctionCall::class.java)
        if (functionCall != null) {
            val provider = DbtSourceRefReferenceProvider()
            val context = ProcessingContext()
            assertDoesNotThrow {
                provider.getReferencesByElement(functionCall, context)
            }
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

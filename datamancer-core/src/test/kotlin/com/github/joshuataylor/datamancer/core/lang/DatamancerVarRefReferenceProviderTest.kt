package com.github.joshuataylor.datamancer.core.lang

import com.intellij.jinja.tags.Jinja2FunctionCall
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.ProcessingContext

/**
 * Tests for DbtVarRefReferenceProvider.
 * Tests the reference provider for dbt var() function calls.
 */
class DbtVarRefReferenceProviderTest : BasePlatformTestCase() {

    // Provider instantiation tests
    fun testProviderCanBeInstantiated() {
        val provider = DbtVarRefReferenceProvider()
        assertNotNull(provider)
    }

    fun testProviderImplementsPsiReferenceProvider() {
        val provider = DbtVarRefReferenceProvider()
        assertTrue(provider is PsiReferenceProvider)
    }

    // Pattern tests
    fun testVarFunctionPatternExists() {
        val pattern = DbtVarRefReferenceProvider.VAR_FUNCTION_PATTERN
        assertNotNull("VAR_FUNCTION_PATTERN should exist", pattern)
    }

    fun testIsVarFunctionConditionExists() {
        val condition = DbtVarRefReferenceProvider.isVarFunction
        assertNotNull("isVarFunction condition should exist", condition)
    }

    // Pattern matching tests using HTML (Jinja2 natively supported)
    fun testIsVarFunctionAcceptsVarCall() {
        val psiFile = myFixture.configureByText("test.html", "{{ var('start_date') }}")
        val functionCall = PsiTreeUtil.findChildOfType(psiFile, Jinja2FunctionCall::class.java)
        if (functionCall != null) {
            val context = ProcessingContext()
            val result = DbtVarRefReferenceProvider.isVarFunction.accepts(functionCall, context)
            assertTrue("Should accept var() function call", result)
        }
    }

    fun testIsVarFunctionRejectsRefCall() {
        val psiFile = myFixture.configureByText("test.html", "{{ ref('customers') }}")
        val functionCall = PsiTreeUtil.findChildOfType(psiFile, Jinja2FunctionCall::class.java)
        if (functionCall != null) {
            val context = ProcessingContext()
            val result = DbtVarRefReferenceProvider.isVarFunction.accepts(functionCall, context)
            assertFalse("Should reject ref() function call", result)
        }
    }

    fun testIsVarFunctionRejectsSourceCall() {
        val psiFile = myFixture.configureByText("test.html", "{{ source('schema', 'table') }}")
        val functionCall = PsiTreeUtil.findChildOfType(psiFile, Jinja2FunctionCall::class.java)
        if (functionCall != null) {
            val context = ProcessingContext()
            val result = DbtVarRefReferenceProvider.isVarFunction.accepts(functionCall, context)
            assertFalse("Should reject source() function call", result)
        }
    }

    fun testIsVarFunctionRejectsConfigCall() {
        val psiFile = myFixture.configureByText("test.html", "{{ config(materialized='table') }}")
        val functionCall = PsiTreeUtil.findChildOfType(psiFile, Jinja2FunctionCall::class.java)
        if (functionCall != null) {
            val context = ProcessingContext()
            val result = DbtVarRefReferenceProvider.isVarFunction.accepts(functionCall, context)
            assertFalse("Should reject config() function call", result)
        }
    }

    fun testIsVarFunctionRejectsUppercaseVar() {
        val psiFile = myFixture.configureByText("test.html", "{{ VAR('variable') }}")
        val functionCall = PsiTreeUtil.findChildOfType(psiFile, Jinja2FunctionCall::class.java)
        if (functionCall != null) {
            val context = ProcessingContext()
            val result = DbtVarRefReferenceProvider.isVarFunction.accepts(functionCall, context)
            assertFalse("Should reject VAR() (uppercase)", result)
        }
    }

    // getReferencesByElement tests - without dbt project configured
    fun testGetReferencesByElementReturnsEmptyWithoutDbtProject() {
        val psiFile = myFixture.configureByText("test.html", "{{ var('my_var') }}")
        val functionCall = PsiTreeUtil.findChildOfType(psiFile, Jinja2FunctionCall::class.java)
        if (functionCall != null) {
            val provider = DbtVarRefReferenceProvider()
            val context = ProcessingContext()
            val references = provider.getReferencesByElement(functionCall, context)
            // Without dbt project configured, should return empty
            assertTrue("Should return empty without dbt project", references.isEmpty())
        }
    }

    // Method existence tests
    fun testGetReferencesByElementMethodExists() {
        val provider = DbtVarRefReferenceProvider()
        val method = provider::class.java.methods.find { it.name == "getReferencesByElement" }
        assertNotNull("getReferencesByElement method should exist", method)
    }

    // Multiple provider instances tests
    fun testMultipleInstancesCanBeCreated() {
        val provider1 = DbtVarRefReferenceProvider()
        val provider2 = DbtVarRefReferenceProvider()

        assertNotNull(provider1)
        assertNotNull(provider2)
        assertNotSame(provider1, provider2)
    }

    // Pattern consistency tests
    fun testPatternIsConsistentAcrossMultipleCalls() {
        val pattern1 = DbtVarRefReferenceProvider.VAR_FUNCTION_PATTERN
        val pattern2 = DbtVarRefReferenceProvider.VAR_FUNCTION_PATTERN
        assertSame("Pattern should be same instance", pattern1, pattern2)
    }

    // Edge cases
    fun testProviderDoesNotThrowOnNonFunctionCallElement() {
        val psiFile = myFixture.configureByText("test.html", "Just some text")
        val provider = DbtVarRefReferenceProvider()
        val context = ProcessingContext()
        assertDoesNotThrow {
            provider.getReferencesByElement(psiFile, context)
        }
    }

    fun testProviderDoesNotThrowOnEmptyVar() {
        val psiFile = myFixture.configureByText("test.html", "{{ var() }}")
        val functionCall = PsiTreeUtil.findChildOfType(psiFile, Jinja2FunctionCall::class.java)
        if (functionCall != null) {
            val provider = DbtVarRefReferenceProvider()
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

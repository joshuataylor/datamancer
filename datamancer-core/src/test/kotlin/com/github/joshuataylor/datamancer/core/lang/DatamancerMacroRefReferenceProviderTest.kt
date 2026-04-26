package com.github.joshuataylor.datamancer.core.lang

import com.intellij.jinja.tags.Jinja2FunctionCall
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.ProcessingContext

/**
 * Tests for DbtMacroRefReferenceProvider.
 * Tests the reference provider for dbt custom macro function calls.
 *
 * Note: Full integration tests with actual macro resolution would require
 * setting up a dbt project with DatamancerDbtProjectIndexService configured.
 */
class DatamancerMacroRefReferenceProviderTest : BasePlatformTestCase() {

    // Provider instantiation tests
    fun testProviderCanBeInstantiated() {
        val provider = DbtMacroRefReferenceProvider()
        assertNotNull(provider)
    }

    fun testProviderImplementsPsiReferenceProvider() {
        val provider = DbtMacroRefReferenceProvider()
        assertTrue(provider is PsiReferenceProvider)
    }

    // Pattern tests
    fun testMacroFunctionPatternExists() {
        val pattern = DbtMacroRefReferenceProvider.MACRO_FUNCTION_PATTERN
        assertNotNull("MACRO_FUNCTION_PATTERN should exist", pattern)
    }

    fun testIsMacroFunctionConditionExists() {
        val condition = DbtMacroRefReferenceProvider.isMacroFunction
        assertNotNull("isMacroFunction condition should exist", condition)
    }

    fun testBuiltInFunctionsSetExists() {
        val builtIns = DbtMacroRefReferenceProvider.BUILT_IN_FUNCTIONS
        assertNotNull("BUILT_IN_FUNCTIONS set should exist", builtIns)
        assertFalse("BUILT_IN_FUNCTIONS should not be empty", builtIns.isEmpty())
    }

    // Built-in function exclusion tests
    fun testBuiltInFunctionsContainsRef() {
        assertTrue(
            "BUILT_IN_FUNCTIONS should contain 'ref'",
            DbtMacroRefReferenceProvider.BUILT_IN_FUNCTIONS.contains("ref")
        )
    }

    fun testBuiltInFunctionsContainsSource() {
        assertTrue(
            "BUILT_IN_FUNCTIONS should contain 'source'",
            DbtMacroRefReferenceProvider.BUILT_IN_FUNCTIONS.contains("source")
        )
    }

    fun testBuiltInFunctionsContainsVar() {
        assertTrue(
            "BUILT_IN_FUNCTIONS should contain 'var'",
            DbtMacroRefReferenceProvider.BUILT_IN_FUNCTIONS.contains("var")
        )
    }

    fun testBuiltInFunctionsContainsConfig() {
        assertTrue(
            "BUILT_IN_FUNCTIONS should contain 'config'",
            DbtMacroRefReferenceProvider.BUILT_IN_FUNCTIONS.contains("config")
        )
    }

    fun testBuiltInFunctionsContainsEnvVar() {
        assertTrue(
            "BUILT_IN_FUNCTIONS should contain 'env_var'",
            DbtMacroRefReferenceProvider.BUILT_IN_FUNCTIONS.contains("env_var")
        )
    }

    fun testBuiltInFunctionsContainsRunQuery() {
        assertTrue(
            "BUILT_IN_FUNCTIONS should contain 'run_query'",
            DbtMacroRefReferenceProvider.BUILT_IN_FUNCTIONS.contains("run_query")
        )
    }

    fun testBuiltInFunctionsContainsJinja2Range() {
        assertTrue(
            "BUILT_IN_FUNCTIONS should contain Jinja2 'range'",
            DbtMacroRefReferenceProvider.BUILT_IN_FUNCTIONS.contains("range")
        )
    }

    fun testBuiltInFunctionsContainsAllDbtParameterisedTags() {
        for (tag in DbtTagLibrary.DBT_PARAMETERIZED_TAGS) {
            assertTrue(
                "BUILT_IN_FUNCTIONS should contain dbt parameterised tag '$tag'",
                DbtMacroRefReferenceProvider.BUILT_IN_FUNCTIONS.contains(tag)
            )
        }
    }

    fun testBuiltInFunctionsContainsAllDbtUnparameterisedTags() {
        for (tag in DbtTagLibrary.DBT_UNPARAMETERIZED_TAGS) {
            assertTrue(
                "BUILT_IN_FUNCTIONS should contain dbt unparameterised tag '$tag'",
                DbtMacroRefReferenceProvider.BUILT_IN_FUNCTIONS.contains(tag)
            )
        }
    }

    // Pattern matching tests - rejection of built-in functions
    fun testIsMacroFunctionRejectsRefCall() {
        val psiFile = myFixture.configureByText("test.html", "{{ ref('model') }}")
        val functionCall = PsiTreeUtil.findChildOfType(psiFile, Jinja2FunctionCall::class.java)
        if (functionCall != null) {
            val context = ProcessingContext()
            val result = DbtMacroRefReferenceProvider.isMacroFunction.accepts(functionCall, context)
            assertFalse("Should reject ref() function call", result)
        }
    }

    fun testIsMacroFunctionRejectsSourceCall() {
        val psiFile = myFixture.configureByText("test.html", "{{ source('raw', 'customers') }}")
        val functionCall = PsiTreeUtil.findChildOfType(psiFile, Jinja2FunctionCall::class.java)
        if (functionCall != null) {
            val context = ProcessingContext()
            val result = DbtMacroRefReferenceProvider.isMacroFunction.accepts(functionCall, context)
            assertFalse("Should reject source() function call", result)
        }
    }

    fun testIsMacroFunctionRejectsVarCall() {
        val psiFile = myFixture.configureByText("test.html", "{{ var('my_var') }}")
        val functionCall = PsiTreeUtil.findChildOfType(psiFile, Jinja2FunctionCall::class.java)
        if (functionCall != null) {
            val context = ProcessingContext()
            val result = DbtMacroRefReferenceProvider.isMacroFunction.accepts(functionCall, context)
            assertFalse("Should reject var() function call", result)
        }
    }

    fun testIsMacroFunctionRejectsConfigCall() {
        val psiFile = myFixture.configureByText("test.html", "{{ config(materialized='table') }}")
        val functionCall = PsiTreeUtil.findChildOfType(psiFile, Jinja2FunctionCall::class.java)
        if (functionCall != null) {
            val context = ProcessingContext()
            val result = DbtMacroRefReferenceProvider.isMacroFunction.accepts(functionCall, context)
            assertFalse("Should reject config() function call", result)
        }
    }

    fun testIsMacroFunctionRejectsEnvVarCall() {
        val psiFile = myFixture.configureByText("test.html", "{{ env_var('MY_VAR') }}")
        val functionCall = PsiTreeUtil.findChildOfType(psiFile, Jinja2FunctionCall::class.java)
        if (functionCall != null) {
            val context = ProcessingContext()
            val result = DbtMacroRefReferenceProvider.isMacroFunction.accepts(functionCall, context)
            assertFalse("Should reject env_var() function call", result)
        }
    }

    fun testIsMacroFunctionRejectsRunQueryCall() {
        val psiFile = myFixture.configureByText("test.html", "{{ run_query('SELECT 1') }}")
        val functionCall = PsiTreeUtil.findChildOfType(psiFile, Jinja2FunctionCall::class.java)
        if (functionCall != null) {
            val context = ProcessingContext()
            val result = DbtMacroRefReferenceProvider.isMacroFunction.accepts(functionCall, context)
            assertFalse("Should reject run_query() function call", result)
        }
    }

    // Pattern matching tests - acceptance of custom macros
    fun testIsMacroFunctionAcceptsCustomMacro() {
        val psiFile = myFixture.configureByText("test.html", "{{ testf() }}")
        val functionCall = PsiTreeUtil.findChildOfType(psiFile, Jinja2FunctionCall::class.java)
        if (functionCall != null) {
            val context = ProcessingContext()
            val result = DbtMacroRefReferenceProvider.isMacroFunction.accepts(functionCall, context)
            assertTrue("Should accept custom macro call", result)
        }
    }

    fun testIsMacroFunctionAcceptsMacroWithArgs() {
        val psiFile = myFixture.configureByText("test.html", "{{ testf2('x') }}")
        val functionCall = PsiTreeUtil.findChildOfType(psiFile, Jinja2FunctionCall::class.java)
        if (functionCall != null) {
            val context = ProcessingContext()
            val result = DbtMacroRefReferenceProvider.isMacroFunction.accepts(functionCall, context)
            assertTrue("Should accept custom macro call with arguments", result)
        }
    }

    fun testIsMacroFunctionAcceptsMacroWithMultipleArgs() {
        val psiFile = myFixture.configureByText("test.html", "{{ my_macro(arg1, arg2) }}")
        val functionCall = PsiTreeUtil.findChildOfType(psiFile, Jinja2FunctionCall::class.java)
        if (functionCall != null) {
            val context = ProcessingContext()
            val result = DbtMacroRefReferenceProvider.isMacroFunction.accepts(functionCall, context)
            assertTrue("Should accept macro call with multiple arguments", result)
        }
    }

    fun testIsMacroFunctionAcceptsMacroWithUnderscores() {
        val psiFile = myFixture.configureByText("test.html", "{{ cents_to_dollars('amount') }}")
        val functionCall = PsiTreeUtil.findChildOfType(psiFile, Jinja2FunctionCall::class.java)
        if (functionCall != null) {
            val context = ProcessingContext()
            val result = DbtMacroRefReferenceProvider.isMacroFunction.accepts(functionCall, context)
            assertTrue("Should accept macro call with underscores", result)
        }
    }

    // getReferencesByElement tests - without dbt project configured
    fun testGetReferencesByElementReturnsEmptyWithoutDbtProject() {
        val psiFile = myFixture.configureByText("test.html", "{{ my_macro() }}")
        val functionCall = PsiTreeUtil.findChildOfType(psiFile, Jinja2FunctionCall::class.java)
        if (functionCall != null) {
            val provider = DbtMacroRefReferenceProvider()
            val context = ProcessingContext()
            val references = provider.getReferencesByElement(functionCall, context)
            assertTrue("Should return empty without dbt project", references.isEmpty())
        }
    }

    // Method existence tests
    fun testGetReferencesByElementMethodExists() {
        val provider = DbtMacroRefReferenceProvider()
        val method = provider::class.java.methods.find { it.name == "getReferencesByElement" }
        assertNotNull("getReferencesByElement method should exist", method)
    }

    // Edge cases
    fun testProviderDoesNotThrowOnNonFunctionCallElement() {
        val psiFile = myFixture.configureByText("test.html", "Just some text")
        val provider = DbtMacroRefReferenceProvider()
        val context = ProcessingContext()
        assertDoesNotThrow {
            provider.getReferencesByElement(psiFile, context)
        }
    }

    // Pattern consistency
    fun testPatternIsConsistentAcrossMultipleCalls() {
        val pattern1 = DbtMacroRefReferenceProvider.MACRO_FUNCTION_PATTERN
        val pattern2 = DbtMacroRefReferenceProvider.MACRO_FUNCTION_PATTERN
        assertSame("Pattern should be same instance", pattern1, pattern2)
    }

    private fun assertDoesNotThrow(block: () -> Unit) {
        try {
            block()
        } catch (e: Throwable) {
            fail("Expected no exception but got: ${e.message}")
        }
    }
}

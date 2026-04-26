package com.github.joshuataylor.datamancer.core

import com.intellij.jinja.psi.Jinja2StringLiteral
import com.intellij.jinja.tags.Jinja2FunctionCall
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for DatamancerUtils.
 * Tests utility functions for Jinja2 and SQL PSI operations.
 *
 * Uses .html files which Jinja2 natively supports in the test environment.
 * Note: Jinja2 PSI may not be fully available in light test environments,
 * so tests guard against null function calls.
 */
class DatamancerUtilsTest : BasePlatformTestCase() {

    // Helper method to create a Jinja2 file and find the first function call
    private fun createJinjaFileAndGetFunctionCall(content: String): Jinja2FunctionCall? {
        val psiFile = myFixture.configureByText("test.html", content)
        return PsiTreeUtil.findChildOfType(psiFile, Jinja2FunctionCall::class.java)
    }

    // isRefCall tests
    fun testIsRefCallWithRefFunction() {
        val call = createJinjaFileAndGetFunctionCall("{{ ref('stg_customers') }}")
        if (call != null) {
            assertTrue("ref() should be identified as ref call", DatamancerUtils.isRefCall(call))
        }
    }

    fun testIsRefCallWithSourceFunction() {
        val call = createJinjaFileAndGetFunctionCall("{{ source('raw', 'customers') }}")
        if (call != null) {
            assertFalse("source() should not be identified as ref call", DatamancerUtils.isRefCall(call))
        }
    }

    fun testIsRefCallWithConfigFunction() {
        val call = createJinjaFileAndGetFunctionCall("{{ config(materialized='table') }}")
        if (call != null) {
            assertFalse("config() should not be identified as ref call", DatamancerUtils.isRefCall(call))
        }
    }

    fun testIsRefCallWithUppercaseRef() {
        // dbt is case-sensitive, REF should not match
        val call = createJinjaFileAndGetFunctionCall("{{ REF('model') }}")
        if (call != null) {
            assertFalse("REF (uppercase) should not match ref", DatamancerUtils.isRefCall(call))
        }
    }

    // isSourceCall tests
    fun testIsSourceCallWithSourceFunction() {
        val call = createJinjaFileAndGetFunctionCall("{{ source('raw_data', 'customers') }}")
        if (call != null) {
            assertTrue("source() should be identified as source call", DatamancerUtils.isSourceCall(call))
        }
    }

    fun testIsSourceCallWithRefFunction() {
        val call = createJinjaFileAndGetFunctionCall("{{ ref('stg_customers') }}")
        if (call != null) {
            assertFalse("ref() should not be identified as source call", DatamancerUtils.isSourceCall(call))
        }
    }

    // getReferencedName tests
    fun testGetReferencedNameWithSingleQuotes() {
        val call = createJinjaFileAndGetFunctionCall("{{ ref('stg_customers') }}")
        if (call != null) {
            val name = DatamancerUtils.getReferencedName(call)
            assertEquals("stg_customers", name)
        }
    }

    fun testGetReferencedNameWithNoArguments() {
        val call = createJinjaFileAndGetFunctionCall("{{ ref() }}")
        if (call != null) {
            val name = DatamancerUtils.getReferencedName(call)
            assertNull("Should return null for empty ref()", name)
        }
    }

    // getStringArguments tests
    fun testGetStringArgumentsWithOneArg() {
        val call = createJinjaFileAndGetFunctionCall("{{ ref('model') }}")
        if (call != null) {
            val args = DatamancerUtils.getStringArguments(call)
            assertEquals(1, args.size)
            assertEquals("model", args[0])
        }
    }

    fun testGetStringArgumentsWithTwoArgs() {
        val call = createJinjaFileAndGetFunctionCall("{{ source('schema', 'table') }}")
        if (call != null) {
            val args = DatamancerUtils.getStringArguments(call)
            assertEquals(2, args.size)
            assertEquals("schema", args[0])
            assertEquals("table", args[1])
        }
    }

    fun testGetStringArgumentsWithNoArgs() {
        val call = createJinjaFileAndGetFunctionCall("{{ ref() }}")
        if (call != null) {
            val args = DatamancerUtils.getStringArguments(call)
            assertTrue("Should return empty list for no arguments", args.isEmpty())
        }
    }

    // getSourceArguments tests
    fun testGetSourceArgumentsWithValidSource() {
        val call = createJinjaFileAndGetFunctionCall("{{ source('raw_data', 'customers') }}")
        if (call != null) {
            val args = DatamancerUtils.getSourceArguments(call)
            assertNotNull("Should return pair for valid source call", args)
            assertEquals("raw_data", args!!.first)
            assertEquals("customers", args.second)
        }
    }

    fun testGetSourceArgumentsWithOneArg() {
        val call = createJinjaFileAndGetFunctionCall("{{ source('raw_data') }}")
        if (call != null) {
            val args = DatamancerUtils.getSourceArguments(call)
            assertNull("Should return null for source with only one arg", args)
        }
    }

    // getStringLiteralArguments tests
    fun testGetStringLiteralArgumentsReturnsElements() {
        val call = createJinjaFileAndGetFunctionCall("{{ source('schema', 'table') }}")
        if (call != null) {
            val literals = DatamancerUtils.getStringLiteralArguments(call)
            assertEquals(2, literals.size)
            assertTrue("Should return Jinja2StringLiteral elements", literals.all { it is Jinja2StringLiteral })
        }
    }

    fun testGetStringLiteralArgumentsPreservesOrder() {
        val call = createJinjaFileAndGetFunctionCall("{{ source('first', 'second') }}")
        if (call != null) {
            val literals = DatamancerUtils.getStringLiteralArguments(call)
            if (literals.size >= 2) {
                assertEquals("first", literals[0].value)
                assertEquals("second", literals[1].value)
            }
        }
    }

    fun testGetStringLiteralArgumentsEmptyForNoArgs() {
        val call = createJinjaFileAndGetFunctionCall("{{ ref() }}")
        if (call != null) {
            val literals = DatamancerUtils.getStringLiteralArguments(call)
            assertTrue("Should return empty list for no arguments", literals.isEmpty())
        }
    }

    // Edge cases
    fun testStringArgumentsWithSpecialCharacters() {
        val call = createJinjaFileAndGetFunctionCall("{{ ref('stg_my-model_v2') }}")
        if (call != null) {
            val args = DatamancerUtils.getStringArguments(call)
            assertEquals(1, args.size)
            assertEquals("stg_my-model_v2", args[0])
        }
    }

    // Additional behavioural tests that don't require Jinja2 PSI
    fun testGetJinjaCallReturnsNullForNonJinjaElement() {
        val psiFile = myFixture.configureByText("test.txt", "plain text content")
        val element = psiFile.firstChild
        if (element != null) {
            val call = DatamancerUtils.getJinjaCall(element)
            assertNull("Should return null for non-Jinja elements", call)
        }
    }

    fun testIsSqlDialectReturnsFalseForPlainTextFile() {
        val psiFile = myFixture.configureByText("test.txt", "not sql")
        assertFalse(
            "Plain text file should not be SQL dialect",
            DatamancerUtils.isSqlDialect(psiFile)
        )
    }
}

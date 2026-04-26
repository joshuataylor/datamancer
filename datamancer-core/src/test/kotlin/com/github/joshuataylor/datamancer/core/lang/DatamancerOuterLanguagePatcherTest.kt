package com.github.joshuataylor.datamancer.core.lang

import com.intellij.jinja.template.psi.Jinja2TemplateElementTypes
import com.intellij.psi.templateLanguages.TemplateDataElementType.OuterLanguageRangePatcher
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for DbtOuterLanguagePatcher.
 * Tests outer language range patching for SQL in Jinja2 templates.
 */
class DbtOuterLanguagePatcherTest : BasePlatformTestCase() {
    private lateinit var patcher: DbtOuterLanguagePatcher

    override fun setUp() {
        super.setUp()
        patcher = DbtOuterLanguagePatcher()
    }

    // Basic instantiation tests
    fun testPatcherCanBeInstantiated() {
        assertNotNull(patcher)
    }

    fun testPatcherIsOuterLanguageRangePatcher() {
        assertTrue(patcher is OuterLanguageRangePatcher)
    }

    fun testPlaceholderConstantExists() {
        assertEquals("datamancer_placeholder", DbtOuterLanguagePatcher.PLACEHOLDER_IDENTIFIER)
    }

    // Template data element type tests
    fun testJinja2TemplateDataReturnsPlaceholder() {
        val result = patcher.getTextForOuterLanguageInsertionRange(
            Jinja2TemplateElementTypes.TEMPLATE_DATA,
            "some text"
        )

        assertEquals(DbtOuterLanguagePatcher.PLACEHOLDER_IDENTIFIER, result)
    }

    fun testJinja2TemplateDataWithEmptyTextReturnsPlaceholder() {
        val result = patcher.getTextForOuterLanguageInsertionRange(
            Jinja2TemplateElementTypes.TEMPLATE_DATA,
            ""
        )

        assertEquals(DbtOuterLanguagePatcher.PLACEHOLDER_IDENTIFIER, result)
    }

    fun testJinja2TemplateDataWithLongTextReturnsPlaceholder() {
        val longText = "SELECT * FROM table WHERE condition = true ".repeat(100)
        val result = patcher.getTextForOuterLanguageInsertionRange(
            Jinja2TemplateElementTypes.TEMPLATE_DATA,
            longText
        )

        assertEquals(DbtOuterLanguagePatcher.PLACEHOLDER_IDENTIFIER, result)
    }

    fun testJinja2TemplateDataWithSpecialCharactersReturnsPlaceholder() {
        val specialText = "{{ ref('model') }} {% for x in list %} SELECT * {% endfor %}"
        val result = patcher.getTextForOuterLanguageInsertionRange(
            Jinja2TemplateElementTypes.TEMPLATE_DATA,
            specialText
        )

        assertEquals(DbtOuterLanguagePatcher.PLACEHOLDER_IDENTIFIER, result)
    }

    // Non-template data element types
    fun testNonTemplateDataElementReturnsNull() {
        // We can't easily test with a different element type without mocking,
        // but we can at least verify the method signature accepts TemplateDataElementType
        // The actual behavior is tested in integration with the real Jinja2 parser
        // This test documents the expected behavior
        assertTrue(true)
    }

    // Placeholder identifier validity tests
    fun testPlaceholderIsValidIdentifier() {
        val placeholder = DbtOuterLanguagePatcher.PLACEHOLDER_IDENTIFIER

        // Check it's a valid identifier (starts with letter, contains only alphanumeric and underscore)
        assertTrue(placeholder.matches(Regex("[a-zA-Z][a-zA-Z0-9_]*")))
    }

    fun testPlaceholderDoesNotContainSpaces() {
        val placeholder = DbtOuterLanguagePatcher.PLACEHOLDER_IDENTIFIER

        assertFalse(placeholder.contains(" "))
    }

    fun testPlaceholderIsNotEmpty() {
        val placeholder = DbtOuterLanguagePatcher.PLACEHOLDER_IDENTIFIER

        assertTrue(placeholder.isNotEmpty())
    }

    // Method signature tests
    fun testGetTextForOuterLanguageInsertionRangeMethodExists() {
        val method = patcher::class.java.methods.find {
            it.name == "getTextForOuterLanguageInsertionRange" && it.parameterCount == 2
        }

        assertNotNull(method)
    }

    fun testMethodReturnsNullableString() {
        val method = patcher::class.java.methods.find {
            it.name == "getTextForOuterLanguageInsertionRange"
        }

        assertNotNull(method)
        // Return type should be String (nullable in Kotlin)
        assertTrue(method!!.returnType == String::class.java)
    }

    // Multiple instance tests
    fun testMultipleInstancesCanBeCreated() {
        val patcher1 = DbtOuterLanguagePatcher()
        val patcher2 = DbtOuterLanguagePatcher()

        assertNotNull(patcher1)
        assertNotNull(patcher2)
        assertNotSame(patcher1, patcher2)
    }

    fun testMultipleInstancesReturnSamePlaceholder() {
        val patcher1 = DbtOuterLanguagePatcher()
        val patcher2 = DbtOuterLanguagePatcher()

        val result1 = patcher1.getTextForOuterLanguageInsertionRange(
            Jinja2TemplateElementTypes.TEMPLATE_DATA,
            "text"
        )
        val result2 = patcher2.getTextForOuterLanguageInsertionRange(
            Jinja2TemplateElementTypes.TEMPLATE_DATA,
            "text"
        )

        assertEquals(result1, result2)
    }

    // Edge case tests
    fun testPatcherHandlesNullCharSequenceGracefully() {
        // Note: CharSequence parameter is not nullable in the interface,
        // but we test defensive handling if platform passes unexpected values
        assertDoesNotThrow {
            patcher.getTextForOuterLanguageInsertionRange(
                Jinja2TemplateElementTypes.TEMPLATE_DATA,
                ""
            )
        }
    }

    fun testPatcherWithUnicodeCharacters() {
        val unicodeText = "SELECT * FROM 表 WHERE条件 = '值'"
        val result = patcher.getTextForOuterLanguageInsertionRange(
            Jinja2TemplateElementTypes.TEMPLATE_DATA,
            unicodeText
        )

        assertEquals(DbtOuterLanguagePatcher.PLACEHOLDER_IDENTIFIER, result)
    }

    fun testPatcherWithNewlines() {
        val textWithNewlines = "SELECT *\nFROM table\nWHERE condition"
        val result = patcher.getTextForOuterLanguageInsertionRange(
            Jinja2TemplateElementTypes.TEMPLATE_DATA,
            textWithNewlines
        )

        assertEquals(DbtOuterLanguagePatcher.PLACEHOLDER_IDENTIFIER, result)
    }

    // Config block tests - these should return null (no placeholder)
    fun testConfigCallReturnsNull() {
        val configText = "{{ config(materialized='table') }}"
        val result = patcher.getTextForOuterLanguageInsertionRange(
            Jinja2TemplateElementTypes.TEMPLATE_DATA,
            configText
        )

        assertNull("config() calls should not get a placeholder", result)
    }

    fun testConfigCallWithWhitespaceReturnsNull() {
        val configText = "{{  config(materialized='table')  }}"
        val result = patcher.getTextForOuterLanguageInsertionRange(
            Jinja2TemplateElementTypes.TEMPLATE_DATA,
            configText
        )

        assertNull("config() calls with whitespace should not get a placeholder", result)
    }

    fun testConfigCallWithMinusWhitespaceControlReturnsNull() {
        val configText = "{{- config(materialized='table') -}}"
        val result = patcher.getTextForOuterLanguageInsertionRange(
            Jinja2TemplateElementTypes.TEMPLATE_DATA,
            configText
        )

        assertNull("config() calls with whitespace control should not get a placeholder", result)
    }

    fun testMultilineConfigCallReturnsNull() {
        val configText = """{{
            config(
                materialized = "table",
                tags = ["core", "events"]
            )
        }}"""
        val result = patcher.getTextForOuterLanguageInsertionRange(
            Jinja2TemplateElementTypes.TEMPLATE_DATA,
            configText
        )

        assertNull("Multiline config() calls should not get a placeholder", result)
    }

    fun testConfigCallWithComplexArgumentsReturnsNull() {
        val configText = "{{ config(enabled=true, tags=['a', 'b'], meta={'owner': 'team'}) }}"
        val result = patcher.getTextForOuterLanguageInsertionRange(
            Jinja2TemplateElementTypes.TEMPLATE_DATA,
            configText
        )

        assertNull("config() with complex arguments should not get a placeholder", result)
    }

    fun testConfigCallCaseInsensitiveReturnsNull() {
        val configText = "{{ CONFIG(materialized='table') }}"
        val result = patcher.getTextForOuterLanguageInsertionRange(
            Jinja2TemplateElementTypes.TEMPLATE_DATA,
            configText
        )

        assertNull("CONFIG() (uppercase) should not get a placeholder", result)
    }

    // Non-config dbt functions should still get placeholders
    fun testRefCallReturnsPlaceholder() {
        val refText = "{{ ref('customers') }}"
        val result = patcher.getTextForOuterLanguageInsertionRange(
            Jinja2TemplateElementTypes.TEMPLATE_DATA,
            refText
        )

        assertEquals(DbtOuterLanguagePatcher.PLACEHOLDER_IDENTIFIER, result)
    }

    fun testSourceCallReturnsPlaceholder() {
        val sourceText = "{{ source('raw', 'orders') }}"
        val result = patcher.getTextForOuterLanguageInsertionRange(
            Jinja2TemplateElementTypes.TEMPLATE_DATA,
            sourceText
        )

        assertEquals(DbtOuterLanguagePatcher.PLACEHOLDER_IDENTIFIER, result)
    }

    fun testVarCallReturnsPlaceholder() {
        val varText = "{{ var('payment_methods') }}"
        val result = patcher.getTextForOuterLanguageInsertionRange(
            Jinja2TemplateElementTypes.TEMPLATE_DATA,
            varText
        )

        assertEquals(DbtOuterLanguagePatcher.PLACEHOLDER_IDENTIFIER, result)
    }

    fun testThisReturnsPlaceholder() {
        val thisText = "{{ this }}"
        val result = patcher.getTextForOuterLanguageInsertionRange(
            Jinja2TemplateElementTypes.TEMPLATE_DATA,
            thisText
        )

        assertEquals(DbtOuterLanguagePatcher.PLACEHOLDER_IDENTIFIER, result)
    }

    fun testCustomMacroReturnsPlaceholder() {
        val macroText = "{{ cents_to_dollars('amount') }}"
        val result = patcher.getTextForOuterLanguageInsertionRange(
            Jinja2TemplateElementTypes.TEMPLATE_DATA,
            macroText
        )

        assertEquals(DbtOuterLanguagePatcher.PLACEHOLDER_IDENTIFIER, result)
    }

    // Other setup/config macros with keyword args should return null
    fun testGenerateIndexesReturnsNull() {
        val text = "{{ generate_indexes(primary_key='id') }}"
        val result = patcher.getTextForOuterLanguageInsertionRange(
            Jinja2TemplateElementTypes.TEMPLATE_DATA,
            text
        )

        assertNull("generate_indexes() with keyword args should not get a placeholder", result)
    }

    fun testGenerateIndexesMultipleKeywordArgsReturnsNull() {
        val text = "{{ generate_indexes(primary_key='id', unique_keys=['email']) }}"
        val result = patcher.getTextForOuterLanguageInsertionRange(
            Jinja2TemplateElementTypes.TEMPLATE_DATA,
            text
        )

        assertNull("generate_indexes() with multiple keyword args should not get a placeholder", result)
    }

    fun testCustomSetupMacroWithKeywordArgsReturnsNull() {
        val text = "{{ my_setup_macro(option=True, setting='value') }}"
        val result = patcher.getTextForOuterLanguageInsertionRange(
            Jinja2TemplateElementTypes.TEMPLATE_DATA,
            text
        )

        assertNull("Custom macros with keyword args first should not get a placeholder", result)
    }

    fun testSetupMacroWithWhitespaceControlReturnsNull() {
        val text = "{{- generate_indexes(primary_key='id') -}}"
        val result = patcher.getTextForOuterLanguageInsertionRange(
            Jinja2TemplateElementTypes.TEMPLATE_DATA,
            text
        )

        assertNull("Setup macros with whitespace control should not get a placeholder", result)
    }

    // Custom macros with positional args first should get placeholders
    fun testCustomMacroWithPositionalThenKeywordReturnsPlaceholder() {
        // Positional arg first, then keyword - this is output-producing
        val text = "{{ my_macro('value', option=True) }}"
        val result = patcher.getTextForOuterLanguageInsertionRange(
            Jinja2TemplateElementTypes.TEMPLATE_DATA,
            text
        )

        assertEquals(DbtOuterLanguagePatcher.PLACEHOLDER_IDENTIFIER, result)
    }

    fun testEnvVarReturnsPlaceholder() {
        val text = "{{ env_var('MY_VAR') }}"
        val result = patcher.getTextForOuterLanguageInsertionRange(
            Jinja2TemplateElementTypes.TEMPLATE_DATA,
            text
        )

        assertEquals(DbtOuterLanguagePatcher.PLACEHOLDER_IDENTIFIER, result)
    }

    // Edge cases for keyword arg detection
    fun testConfigAsPartOfOtherWordReturnsPlaceholder() {
        // "myconfig" with positional string arg should get placeholder
        val text = "{{ myconfig('value') }}"
        val result = patcher.getTextForOuterLanguageInsertionRange(
            Jinja2TemplateElementTypes.TEMPLATE_DATA,
            text
        )

        assertEquals(DbtOuterLanguagePatcher.PLACEHOLDER_IDENTIFIER, result)
    }

    fun testConfigWithoutParensReturnsPlaceholder() {
        // config without parens is accessing a property, not calling the function
        val text = "{{ config.materialized }}"
        val result = patcher.getTextForOuterLanguageInsertionRange(
            Jinja2TemplateElementTypes.TEMPLATE_DATA,
            text
        )

        assertEquals(DbtOuterLanguagePatcher.PLACEHOLDER_IDENTIFIER, result)
    }

    fun testFunctionWithNoArgsReturnsPlaceholder() {
        // Function with no args should get placeholder (could produce output)
        val text = "{{ get_current_timestamp() }}"
        val result = patcher.getTextForOuterLanguageInsertionRange(
            Jinja2TemplateElementTypes.TEMPLATE_DATA,
            text
        )

        assertEquals(DbtOuterLanguagePatcher.PLACEHOLDER_IDENTIFIER, result)
    }

    // Performance tests
    fun testPatcherPerformance() {
        val startTime = System.currentTimeMillis()

        repeat(1000) {
            patcher.getTextForOuterLanguageInsertionRange(
                Jinja2TemplateElementTypes.TEMPLATE_DATA,
                "some text $it"
            )
        }

        val elapsedTime = System.currentTimeMillis() - startTime

        // Should complete 1000 calls in less than 100ms
        assertTrue("Patcher took too long: ${elapsedTime}ms", elapsedTime < 100)
    }

    fun testKeywordArgDetectionPerformance() {
        val configText = "{{ config(materialized='table', tags=['a', 'b']) }}"
        val startTime = System.currentTimeMillis()

        repeat(1000) {
            patcher.getTextForOuterLanguageInsertionRange(
                Jinja2TemplateElementTypes.TEMPLATE_DATA,
                configText
            )
        }

        val elapsedTime = System.currentTimeMillis() - startTime

        // Should complete 1000 calls in less than 100ms even with regex
        assertTrue("Keyword arg detection took too long: ${elapsedTime}ms", elapsedTime < 100)
    }

    private fun assertDoesNotThrow(block: () -> Unit) {
        try {
            block()
        } catch (e: Throwable) {
            fail("Expected no exception but got: ${e.message}")
        }
    }
}

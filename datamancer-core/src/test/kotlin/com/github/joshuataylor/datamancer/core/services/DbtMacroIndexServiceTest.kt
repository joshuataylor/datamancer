package com.github.joshuataylor.datamancer.core.services

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for DbtMacroIndexService.
 * Tests macro indexing and lookup from .sql files in macros/ directories.
 *
 * Note: Full integration tests with actual macro resolution would require
 * setting up a dbt project with DatamancerDbtProjectIndexService configured.
 */
class DbtMacroIndexServiceTest : BasePlatformTestCase() {

    // Service instantiation tests
    fun testServiceInstanceCreation() {
        val service = DbtMacroIndexService.getInstance(project)
        assertNotNull(service)
    }

    fun testServiceIsSingleton() {
        val service1 = DbtMacroIndexService.getInstance(project)
        val service2 = DbtMacroIndexService.getInstance(project)
        assertSame(service1, service2)
    }

    fun testServiceExistsInProject() {
        val service = project.getService(DbtMacroIndexService::class.java)
        assertNotNull(service)
    }

    // Method existence tests
    fun testGetAllMacrosMethodExists() {
        val service = DbtMacroIndexService.getInstance(project)
        val method = service::class.java.methods.find { it.name == "getAllMacros" }
        assertNotNull("getAllMacros method should exist", method)
    }

    fun testFindMacroMethodExists() {
        val service = DbtMacroIndexService.getInstance(project)
        val method = service::class.java.methods.find { it.name == "findMacro" }
        assertNotNull("findMacro method should exist", method)
    }

    fun testGetAllMacroNamesMethodExists() {
        val service = DbtMacroIndexService.getInstance(project)
        val method = service::class.java.methods.find { it.name == "getAllMacroNames" }
        assertNotNull("getAllMacroNames method should exist", method)
    }

    // Empty project tests
    fun testGetAllMacrosReturnsEmptyWithoutDbtProject() {
        val service = DbtMacroIndexService.getInstance(project)
        val macros = service.getAllMacros()
        assertNotNull(macros)
        assertTrue("Should return empty map when no dbt project configured", macros.isEmpty())
    }

    fun testFindMacroReturnsNullWithoutDbtProject() {
        val service = DbtMacroIndexService.getInstance(project)
        val macroDef = service.findMacro("testf")
        assertNull("Should return null when no dbt project configured", macroDef)
    }

    fun testGetAllMacroNamesReturnsEmptyWithoutDbtProject() {
        val service = DbtMacroIndexService.getInstance(project)
        val names = service.getAllMacroNames()
        assertNotNull(names)
        assertTrue("Should return empty list when no dbt project configured", names.isEmpty())
    }

    // Regex tests
    fun testMacroDefinitionRegexMatchesSimpleMacro() {
        val text = "{% macro testf() %}"
        val match = DbtMacroIndexService.MACRO_DEFINITION_REGEX.find(text)
        assertNotNull("Should match simple macro definition", match)
        assertEquals("testf", match!!.groupValues[1])
    }

    fun testMacroDefinitionRegexMatchesParameterisedMacro() {
        val text = "{% macro testf2(hello) %}"
        val match = DbtMacroIndexService.MACRO_DEFINITION_REGEX.find(text)
        assertNotNull("Should match parameterised macro", match)
        assertEquals("testf2", match!!.groupValues[1])
    }

    fun testMacroDefinitionRegexMatchesMultipleParameters() {
        val text = "{% macro my_macro(arg1, arg2, arg3) %}"
        val match = DbtMacroIndexService.MACRO_DEFINITION_REGEX.find(text)
        assertNotNull("Should match macro with multiple parameters", match)
        assertEquals("my_macro", match!!.groupValues[1])
    }

    fun testMacroDefinitionRegexMatchesDefaultParameters() {
        val text = "{% macro cents_to_dollars(column_name, scale=2) %}"
        val match = DbtMacroIndexService.MACRO_DEFINITION_REGEX.find(text)
        assertNotNull("Should match macro with default parameters", match)
        assertEquals("cents_to_dollars", match!!.groupValues[1])
    }

    fun testMacroDefinitionRegexMatchesDashSyntax() {
        val text = "{%- macro my_macro() -%}"
        val match = DbtMacroIndexService.MACRO_DEFINITION_REGEX.find(text)
        assertNotNull("Should match macro with whitespace control dashes", match)
        assertEquals("my_macro", match!!.groupValues[1])
    }

    fun testMacroDefinitionRegexMatchesLeadingDashOnly() {
        val text = "{%- macro my_macro() %}"
        val match = DbtMacroIndexService.MACRO_DEFINITION_REGEX.find(text)
        assertNotNull("Should match macro with leading dash only", match)
        assertEquals("my_macro", match!!.groupValues[1])
    }

    fun testMacroDefinitionRegexDoesNotMatchEndmacro() {
        val text = "{% endmacro %}"
        val match = DbtMacroIndexService.MACRO_DEFINITION_REGEX.find(text)
        assertNull("Should not match endmacro tag", match)
    }

    fun testMacroDefinitionRegexDoesNotMatchIfTag() {
        val text = "{% if condition %}"
        val match = DbtMacroIndexService.MACRO_DEFINITION_REGEX.find(text)
        assertNull("Should not match if tag", match)
    }

    fun testMacroDefinitionRegexDoesNotMatchForTag() {
        val text = "{% for item in items %}"
        val match = DbtMacroIndexService.MACRO_DEFINITION_REGEX.find(text)
        assertNull("Should not match for tag", match)
    }

    fun testMacroDefinitionRegexDoesNotMatchComment() {
        val text = "{# macro fake_comment() #}"
        val match = DbtMacroIndexService.MACRO_DEFINITION_REGEX.find(text)
        assertNull("Should not match Jinja2 comment", match)
    }

    fun testMacroDefinitionRegexFindsMultipleMacrosInOneFile() {
        val text = """
            {% macro first_macro() %}
              SELECT 1;
            {% endmacro %}

            {% macro second_macro(arg1) %}
              SELECT {{ arg1 }};
            {% endmacro %}
        """.trimIndent()

        val matches = DbtMacroIndexService.MACRO_DEFINITION_REGEX.findAll(text).toList()
        assertEquals("Should find two macro definitions", 2, matches.size)
        assertEquals("first_macro", matches[0].groupValues[1])
        assertEquals("second_macro", matches[1].groupValues[1])
    }

    fun testMacroDefinitionRegexMatchesWithExtraWhitespace() {
        val text = "{%   macro   my_macro   () %}"
        val match = DbtMacroIndexService.MACRO_DEFINITION_REGEX.find(text)
        assertNotNull("Should match macro with extra whitespace", match)
        assertEquals("my_macro", match!!.groupValues[1])
    }

    fun testMacroDefinitionRegexCapturesCorrectOffset() {
        val text = "{% macro testf() %}"
        val match = DbtMacroIndexService.MACRO_DEFINITION_REGEX.find(text)
        assertNotNull(match)
        val macroName = match!!.groupValues[1]
        val nameOffset = match.range.first + match.value.indexOf(macroName)
        assertEquals("Macro name offset should point to 'testf'", 9, nameOffset)
        assertEquals("testf", text.substring(nameOffset, nameOffset + macroName.length))
    }

    // Data class tests
    fun testMacroDefinitionDataClass() {
        val clazz = DbtMacroIndexService.MacroDefinition::class
        assertNotNull(clazz)

        val constructor = clazz.constructors.firstOrNull()
        assertNotNull("MacroDefinition should have a constructor", constructor)

        val paramNames = constructor?.parameters?.map { it.name }
        assertTrue("Should have 'name' parameter", paramNames?.contains("name") == true)
        assertTrue("Should have 'file' parameter", paramNames?.contains("file") == true)
        assertTrue("Should have 'textOffset' parameter", paramNames?.contains("textOffset") == true)
        assertTrue("Should have 'nameLength' parameter", paramNames?.contains("nameLength") == true)
        assertTrue("Should have 'parameters' parameter", paramNames?.contains("parameters") == true)
    }

    // Edge cases
    fun testFindMacroWithEmptyName() {
        val service = DbtMacroIndexService.getInstance(project)
        val macroDef = service.findMacro("")
        assertNull("Should return null for empty macro name", macroDef)
    }

    fun testServiceHandlesGracefully() {
        val service = DbtMacroIndexService.getInstance(project)

        assertDoesNotThrow { service.getAllMacros() }
        assertDoesNotThrow { service.getAllMacroNames() }
        assertDoesNotThrow { service.findMacro("any") }
        assertDoesNotThrow { service.findMacro("") }
    }

    private fun assertDoesNotThrow(block: () -> Unit) {
        try {
            block()
        } catch (e: Throwable) {
            fail("Expected no exception but got: ${e.message}")
        }
    }
}

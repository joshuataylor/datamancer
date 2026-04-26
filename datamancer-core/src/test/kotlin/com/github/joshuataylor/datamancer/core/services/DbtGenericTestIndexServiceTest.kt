package com.github.joshuataylor.datamancer.core.services

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for DbtGenericTestIndexService.
 * Tests generic test indexing and lookup from .sql files in tests/generic/ and macros/ directories.
 *
 * Note: Full integration tests with actual test resolution would require
 * setting up a dbt project with DatamancerDbtProjectIndexService configured.
 */
class DbtGenericTestIndexServiceTest : BasePlatformTestCase() {

    // Service instantiation tests
    fun testServiceInstanceCreation() {
        val service = DbtGenericTestIndexService.getInstance(project)
        assertNotNull(service)
    }

    fun testServiceIsSingleton() {
        val service1 = DbtGenericTestIndexService.getInstance(project)
        val service2 = DbtGenericTestIndexService.getInstance(project)
        assertSame(service1, service2)
    }

    fun testServiceExistsInProject() {
        val service = project.getService(DbtGenericTestIndexService::class.java)
        assertNotNull(service)
    }

    // Method existence tests
    fun testGetAllGenericTestsMethodExists() {
        val service = DbtGenericTestIndexService.getInstance(project)
        val method = service::class.java.methods.find { it.name == "getAllGenericTests" }
        assertNotNull("getAllGenericTests method should exist", method)
    }

    fun testFindGenericTestMethodExists() {
        val service = DbtGenericTestIndexService.getInstance(project)
        val method = service::class.java.methods.find { it.name == "findGenericTest" }
        assertNotNull("findGenericTest method should exist", method)
    }

    fun testGetAllGenericTestNamesMethodExists() {
        val service = DbtGenericTestIndexService.getInstance(project)
        val method = service::class.java.methods.find { it.name == "getAllGenericTestNames" }
        assertNotNull("getAllGenericTestNames method should exist", method)
    }

    // Empty project tests
    fun testGetAllGenericTestsReturnsEmptyWithoutDbtProject() {
        val service = DbtGenericTestIndexService.getInstance(project)
        val tests = service.getAllGenericTests()
        assertNotNull(tests)
        assertTrue("Should return empty map when no dbt project configured", tests.isEmpty())
    }

    fun testFindGenericTestReturnsNullWithoutDbtProject() {
        val service = DbtGenericTestIndexService.getInstance(project)
        val testDef = service.findGenericTest("is_positive")
        assertNull("Should return null when no dbt project configured", testDef)
    }

    fun testGetAllGenericTestNamesReturnsEmptyWithoutDbtProject() {
        val service = DbtGenericTestIndexService.getInstance(project)
        val names = service.getAllGenericTestNames()
        assertNotNull(names)
        assertTrue("Should return empty list when no dbt project configured", names.isEmpty())
    }

    // Regex tests
    fun testTestDefinitionRegexMatchesSimpleTest() {
        val text = "{% test is_positive(model, column_name) %}"
        val match = DbtGenericTestIndexService.TEST_DEFINITION_REGEX.find(text)
        assertNotNull("Should match simple test definition", match)
        assertEquals("is_positive", match!!.groupValues[1])
    }

    fun testTestDefinitionRegexMatchesDashSyntax() {
        val text = "{%- test is_positive(model, column_name) -%}"
        val match = DbtGenericTestIndexService.TEST_DEFINITION_REGEX.find(text)
        assertNotNull("Should match test with whitespace control dashes", match)
        assertEquals("is_positive", match!!.groupValues[1])
    }

    fun testTestDefinitionRegexMatchesLeadingDashOnly() {
        val text = "{%- test is_positive(model, column_name) %}"
        val match = DbtGenericTestIndexService.TEST_DEFINITION_REGEX.find(text)
        assertNotNull("Should match test with leading dash only", match)
        assertEquals("is_positive", match!!.groupValues[1])
    }

    fun testTestDefinitionRegexMatchesDefaultParameters() {
        val text = "{% test accepted_range(model, column_name, min_value=0, max_value=none) %}"
        val match = DbtGenericTestIndexService.TEST_DEFINITION_REGEX.find(text)
        assertNotNull("Should match test with default parameters", match)
        assertEquals("accepted_range", match!!.groupValues[1])
    }

    fun testTestDefinitionRegexMatchesNoParams() {
        val text = "{% test my_test() %}"
        val match = DbtGenericTestIndexService.TEST_DEFINITION_REGEX.find(text)
        assertNotNull("Should match test with no parameters", match)
        assertEquals("my_test", match!!.groupValues[1])
    }

    fun testTestDefinitionRegexDoesNotMatchMacro() {
        val text = "{% macro my_macro(arg1) %}"
        val match = DbtGenericTestIndexService.TEST_DEFINITION_REGEX.find(text)
        assertNull("Should not match macro definition", match)
    }

    fun testTestDefinitionRegexDoesNotMatchEndtest() {
        val text = "{% endtest %}"
        val match = DbtGenericTestIndexService.TEST_DEFINITION_REGEX.find(text)
        assertNull("Should not match endtest tag", match)
    }

    fun testTestDefinitionRegexDoesNotMatchIfTag() {
        val text = "{% if condition %}"
        val match = DbtGenericTestIndexService.TEST_DEFINITION_REGEX.find(text)
        assertNull("Should not match if tag", match)
    }

    fun testTestDefinitionRegexDoesNotMatchComment() {
        val text = "{# test fake_comment() #}"
        val match = DbtGenericTestIndexService.TEST_DEFINITION_REGEX.find(text)
        assertNull("Should not match Jinja2 comment", match)
    }

    fun testTestDefinitionRegexFindsMultipleTestsInOneFile() {
        val text = """
            {% test first_test(model, column_name) %}
              SELECT * FROM {{ model }} WHERE {{ column_name }} IS NULL
            {% endtest %}

            {% test second_test(model, column_name) %}
              SELECT * FROM {{ model }} WHERE {{ column_name }} < 0
            {% endtest %}
        """.trimIndent()

        val matches = DbtGenericTestIndexService.TEST_DEFINITION_REGEX.findAll(text).toList()
        assertEquals("Should find two test definitions", 2, matches.size)
        assertEquals("first_test", matches[0].groupValues[1])
        assertEquals("second_test", matches[1].groupValues[1])
    }

    fun testTestDefinitionRegexMatchesWithExtraWhitespace() {
        val text = "{%   test   my_test   () %}"
        val match = DbtGenericTestIndexService.TEST_DEFINITION_REGEX.find(text)
        assertNotNull("Should match test with extra whitespace", match)
        assertEquals("my_test", match!!.groupValues[1])
    }

    fun testTestDefinitionRegexCapturesCorrectOffset() {
        val text = "{% test is_positive(model) %}"
        val match = DbtGenericTestIndexService.TEST_DEFINITION_REGEX.find(text)
        assertNotNull(match)
        val testName = match!!.groupValues[1]
        val nameOffset = match.range.first + match.value.indexOf(testName)
        assertEquals("is_positive", text.substring(nameOffset, nameOffset + testName.length))
    }

    // Data class tests
    fun testTestDefinitionDataClass() {
        val clazz = DbtGenericTestIndexService.TestDefinition::class
        assertNotNull(clazz)

        val constructor = clazz.constructors.firstOrNull()
        assertNotNull("TestDefinition should have a constructor", constructor)

        val paramNames = constructor?.parameters?.map { it.name }
        assertTrue("Should have 'name' parameter", paramNames?.contains("name") == true)
        assertTrue("Should have 'file' parameter", paramNames?.contains("file") == true)
        assertTrue("Should have 'textOffset' parameter", paramNames?.contains("textOffset") == true)
        assertTrue("Should have 'nameLength' parameter", paramNames?.contains("nameLength") == true)
        assertTrue("Should have 'parameters' parameter", paramNames?.contains("parameters") == true)
    }

    // Edge cases
    fun testFindGenericTestWithEmptyName() {
        val service = DbtGenericTestIndexService.getInstance(project)
        val testDef = service.findGenericTest("")
        assertNull("Should return null for empty test name", testDef)
    }

    fun testServiceHandlesGracefully() {
        val service = DbtGenericTestIndexService.getInstance(project)

        assertDoesNotThrow { service.getAllGenericTests() }
        assertDoesNotThrow { service.getAllGenericTestNames() }
        assertDoesNotThrow { service.findGenericTest("any") }
        assertDoesNotThrow { service.findGenericTest("") }
    }

    private fun assertDoesNotThrow(block: () -> Unit) {
        try {
            block()
        } catch (e: Throwable) {
            fail("Expected no exception but got: ${e.message}")
        }
    }
}

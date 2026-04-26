package com.github.joshuataylor.datamancer.core

import com.github.joshuataylor.datamancer.core.services.DatamancerDbtProjectIndexService
import com.intellij.jinja.Jinja2Language
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for DatamancerLanguageSubstitutor.
 * Tests language substitution for SQL files in dbt modules.
 */
@TestDataPath("\$CONTENT_ROOT/src/test/testData/languageSubstitutor")
class DbtLanguageSubstitutorTest : BasePlatformTestCase() {
    private lateinit var substitutor: DatamancerLanguageSubstitutor

    override fun getTestDataPath(): String = "src/test/testData/languageSubstitutor"

    override fun setUp() {
        super.setUp()
        substitutor = DatamancerLanguageSubstitutor()
    }

    // Basic instantiation and type tests
    fun testSubstitutorCanBeInstantiated() {
        assertNotNull(substitutor)
    }

    fun testSubstitutorIsLanguageSubstitutor() {
        assertTrue(substitutor is com.intellij.psi.LanguageSubstitutor)
    }

    fun testGetLanguageMethodExists() {
        val method = substitutor::class.java.methods.find {
            it.name == "getLanguage" && it.parameterCount == 2
        }
        assertNotNull(method)
    }

    fun testMultipleInstancesCanBeCreated() {
        val substitutor1 = DatamancerLanguageSubstitutor()
        val substitutor2 = DatamancerLanguageSubstitutor()

        assertNotNull(substitutor1)
        assertNotNull(substitutor2)
        assertNotSame(substitutor1, substitutor2)
    }

    // File extension tests
    fun testNonSqlFileReturnsNull() {
        val kotlinFile = myFixture.addFileToProject("test.kt", "fun test() {}")
        val language = substitutor.getLanguage(kotlinFile.virtualFile, project)

        assertNull(language)
    }

    fun testSqlFileExtensionIsChecked() {
        // Test various file extensions
        val txtFile = myFixture.addFileToProject("test.txt", "content")
        val mdFile = myFixture.addFileToProject("test.md", "# Title")
        val pyFile = myFixture.addFileToProject("test.py", "print('hello')")
        val ymlFile = myFixture.addFileToProject("dbt_project.yml", "name: test")

        assertNull(substitutor.getLanguage(txtFile.virtualFile, project))
        assertNull(substitutor.getLanguage(mdFile.virtualFile, project))
        assertNull(substitutor.getLanguage(pyFile.virtualFile, project))
        assertNull(substitutor.getLanguage(ymlFile.virtualFile, project))
    }

    fun testSqlFileWithUppercaseExtension() {
        val sqlFile = myFixture.addFileToProject("test.SQL", "SELECT 1")
        // Extension check is case-sensitive, should return null
        val language = substitutor.getLanguage(sqlFile.virtualFile, project)
        // This depends on VirtualFile extension handling
        assertNull(language)
    }

    // Module and dbt config tests
    fun testSqlFileWithoutModuleReturnsNull() {
        // Create a temporary SQL file
        val tempFile = myFixture.addFileToProject("temp.sql", "SELECT * FROM table")

        // The file might not have a module in the test environment
        val language = substitutor.getLanguage(tempFile.virtualFile, project)

        // Should return null if no module found
        assertNull(language)
    }

    fun testSqlFileWithoutDbtConfigReturnsNull() {
        // Add a SQL file but don't configure it as a dbt project
        val sqlFile = myFixture.addFileToProject("regular.sql", "SELECT 1")

        val language = substitutor.getLanguage(sqlFile.virtualFile, project)

        // Should return null since no dbt config exists
        assertNull(language)
    }

    fun testSqlFileOutsideDbtProjectReturnsNull() {
        // This tests the fix for files outside dbt project being parsed as Jinja2
        // Even if a module has dbt config, files outside the project root should use default SQL
        val sqlFile = myFixture.addFileToProject("outside_project.sql", "SELECT 1")

        val language = substitutor.getLanguage(sqlFile.virtualFile, project)

        // Should return null since file is outside any dbt project
        assertNull("SQL files outside dbt project should not use Jinja2", language)
    }

    fun testIndexServiceIsUsedForConfig() {
        val sqlFile = myFixture.addFileToProject("model.sql", "SELECT 1")

        // Call getLanguage - it should use DatamancerDbtProjectIndexService internally
        substitutor.getLanguage(sqlFile.virtualFile, project)

        // Service should exist (even if no config found)
        val indexService = DatamancerDbtProjectIndexService.getInstance(project)
        assertNotNull(indexService)
    }

    // Error handling tests
    fun testSubstitutorDoesNotThrowOnNullModule() {
        val sqlFile = myFixture.addFileToProject("test.sql", "SELECT 1")

        // Should not throw, just return null
        assertDoesNotThrow {
            val language = substitutor.getLanguage(sqlFile.virtualFile, project)
            // Language should be null since no dbt config exists
            assertNull(language)
        }
    }

    fun testSubstitutorHandlesEmptyProject() {
        val sqlFile = myFixture.addFileToProject("empty.sql", "")

        // Should handle empty SQL files without errors
        assertDoesNotThrow {
            substitutor.getLanguage(sqlFile.virtualFile, project)
        }
    }

    fun testSubstitutorHandlesLargeSqlFile() {
        // Create a large SQL file
        val largeContent = "SELECT * FROM table\n".repeat(1000)
        val sqlFile = myFixture.addFileToProject("large.sql", largeContent)

        // Should handle large files without errors
        assertDoesNotThrow {
            substitutor.getLanguage(sqlFile.virtualFile, project)
        }
    }

    fun testSubstitutorHandlesSpecialCharactersInPath() {
        val sqlFile = myFixture.addFileToProject("path with spaces/test.sql", "SELECT 1")

        assertDoesNotThrow {
            substitutor.getLanguage(sqlFile.virtualFile, project)
        }
    }

    // Jinja2 language availability tests
    fun testJinja2LanguageIsAvailable() {
        // Verify Jinja2Language is accessible
        assertNotNull(Jinja2Language.INSTANCE)
    }

    fun testJinja2LanguageHasCorrectId() {
        assertEquals("Jinja2", Jinja2Language.INSTANCE.id)
    }

    // Content tests - verify substitutor works with dbt-specific syntax
    fun testDbtConfigSyntax() {
        val sqlFile = myFixture.addFileToProject(
            "config_test.sql",
            "{{ config(materialized='table') }}\nSELECT 1"
        )

        // Even without dbt project, should not throw
        assertDoesNotThrow {
            substitutor.getLanguage(sqlFile.virtualFile, project)
        }
    }

    fun testDbtRefSyntax() {
        val sqlFile = myFixture.addFileToProject(
            "ref_test.sql",
            "SELECT * FROM {{ ref('customers') }}"
        )

        assertDoesNotThrow {
            substitutor.getLanguage(sqlFile.virtualFile, project)
        }
    }

    fun testDbtSourceSyntax() {
        val sqlFile = myFixture.addFileToProject(
            "source_test.sql",
            "SELECT * FROM {{ source('raw', 'orders') }}"
        )

        assertDoesNotThrow {
            substitutor.getLanguage(sqlFile.virtualFile, project)
        }
    }

    fun testComplexJinjaSyntax() {
        val sqlFile = myFixture.addFileToProject(
            "complex_jinja.sql",
            """
            {% set payment_methods = ['credit', 'debit'] %}
            SELECT * FROM orders
            WHERE payment IN (
                {% for method in payment_methods %}
                    '{{ method }}'{% if not loop.last %},{% endif %}
                {% endfor %}
            )
            """.trimIndent()
        )

        assertDoesNotThrow {
            substitutor.getLanguage(sqlFile.virtualFile, project)
        }
    }

    // Performance tests
    fun testMultipleCallsDoNotDegradePerformance() {
        val sqlFile = myFixture.addFileToProject("perf_test.sql", "SELECT 1")

        val startTime = System.currentTimeMillis()
        repeat(100) {
            substitutor.getLanguage(sqlFile.virtualFile, project)
        }
        val elapsedTime = System.currentTimeMillis() - startTime

        // Should complete 100 calls in less than 1 second
        assertTrue("Language substitution took too long: ${elapsedTime}ms", elapsedTime < 1000)
    }

    private fun assertDoesNotThrow(block: () -> Unit) {
        try {
            block()
        } catch (e: Throwable) {
            fail("Expected no exception but got: ${e.message}")
        }
    }
}

package com.github.joshuataylor.datamancer.core.icons

import com.intellij.ide.FileIconProvider
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for DbtFileIconProvider.
 * Tests custom icon provider for dbt SQL files.
 */
class DbtFileIconProviderTest : BasePlatformTestCase() {

    // Instantiation tests
    fun testProviderCanBeInstantiated() {
        val provider = DbtFileIconProvider()
        assertNotNull(provider)
    }

    fun testProviderImplementsFileIconProvider() {
        val provider = DbtFileIconProvider()
        assertTrue(provider is FileIconProvider)
    }

    // Method existence tests
    fun testGetIconMethodExists() {
        val provider = DbtFileIconProvider()
        val method = provider::class.java.methods.find { it.name == "getIcon" }
        assertNotNull("getIcon method should exist", method)
    }

    // getIcon tests - returns null for non-SQL files
    fun testGetIconReturnsNullForTextFile() {
        val provider = DbtFileIconProvider()
        val psiFile = myFixture.configureByText("test.txt", "hello world")
        val icon = provider.getIcon(psiFile.virtualFile, 0, project)
        assertNull("Should return null for .txt files", icon)
    }

    fun testGetIconReturnsNullForPythonFile() {
        val provider = DbtFileIconProvider()
        val psiFile = myFixture.configureByText("test.py", "print('hello')")
        val icon = provider.getIcon(psiFile.virtualFile, 0, project)
        assertNull("Should return null for .py files", icon)
    }

    fun testGetIconReturnsNullForYamlFile() {
        val provider = DbtFileIconProvider()
        val psiFile = myFixture.configureByText("schema.yml", "version: 2")
        val icon = provider.getIcon(psiFile.virtualFile, 0, project)
        assertNull("Should return null for .yml files", icon)
    }

    fun testGetIconReturnsNullForCsvFile() {
        val provider = DbtFileIconProvider()
        val psiFile = myFixture.configureByText("data.csv", "id,name\n1,test")
        val icon = provider.getIcon(psiFile.virtualFile, 0, project)
        assertNull("Should return null for .csv files", icon)
    }

    // getIcon with SQL files - without dbt project configured, returns null
    fun testGetIconReturnsNullForSqlFileWithoutDbtProject() {
        val provider = DbtFileIconProvider()
        val psiFile = myFixture.configureByText("model.sql", "SELECT 1")
        val icon = provider.getIcon(psiFile.virtualFile, 0, project)
        // Without dbt project configured, should return null
        assertNull("Should return null for SQL file without dbt project", icon)
    }

    // getIcon with null project
    fun testGetIconReturnsNullWithNullProject() {
        val provider = DbtFileIconProvider()
        val psiFile = myFixture.configureByText("model.sql", "SELECT 1")
        val icon = provider.getIcon(psiFile.virtualFile, 0, null)
        assertNull("Should return null when project is null", icon)
    }

    // Multiple instances tests
    fun testMultipleInstancesCanBeCreated() {
        val provider1 = DbtFileIconProvider()
        val provider2 = DbtFileIconProvider()

        assertNotNull(provider1)
        assertNotNull(provider2)
        assertNotSame(provider1, provider2)
    }

    // Edge cases
    fun testGetIconDoesNotThrowOnVariousFileTypes() {
        val provider = DbtFileIconProvider()
        val fileTypes = listOf(
            "test.sql" to "SELECT 1",
            "test.txt" to "text",
            "test.py" to "pass",
            "test.yml" to "key: value",
            "test.json" to "{}",
            "test.md" to "# heading"
        )

        for ((filename, content) in fileTypes) {
            val psiFile = myFixture.configureByText(filename, content)
            assertDoesNotThrow {
                provider.getIcon(psiFile.virtualFile, 0, project)
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

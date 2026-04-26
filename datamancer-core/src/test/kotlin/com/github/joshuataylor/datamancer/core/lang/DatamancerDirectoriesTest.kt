package com.github.joshuataylor.datamancer.core.lang

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for DbtDirectories.
 * Tests model and seed file discovery utilities.
 *
 * Note: Full integration tests with actual dbt project discovery would require
 * setting up DatamancerDbtProjectIndexService with test data. These tests focus on
 * method existence and empty project behavior.
 */
class DbtDirectoriesTest : BasePlatformTestCase() {

    // Object existence tests
    fun testDirectoriesObjectExists() {
        assertNotNull(DbtDirectories)
    }

    // Method existence tests
    fun testFindModelMethodExists() {
        val method = DbtDirectories::class.java.methods.find { it.name == "findModel" }
        assertNotNull("findModel method should exist", method)
    }

    fun testFindSeedFileMethodExists() {
        val method = DbtDirectories::class.java.methods.find { it.name == "findSeedFile" }
        assertNotNull("findSeedFile method should exist", method)
    }

    fun testFindAllModelsMethodExists() {
        val method = DbtDirectories::class.java.methods.find { it.name == "findAllModels" }
        assertNotNull("findAllModels method should exist", method)
    }

    fun testFindAllSeedsMethodExists() {
        val method = DbtDirectories::class.java.methods.find { it.name == "findAllSeeds" }
        assertNotNull("findAllSeeds method should exist", method)
    }

    fun testGetModelNameFromFileMethodExists() {
        val method = DbtDirectories::class.java.methods.find { it.name == "getModelNameFromFile" }
        assertNotNull("getModelNameFromFile method should exist", method)
    }

    fun testGetSeedNameFromFileMethodExists() {
        val method = DbtDirectories::class.java.methods.find { it.name == "getSeedNameFromFile" }
        assertNotNull("getSeedNameFromFile method should exist", method)
    }

    // Empty project tests - no dbt configs registered
    fun testFindModelReturnsNullWithoutDbtProject() {
        val result = DbtDirectories.findModel(project, "stg_customers")
        assertNull("Should return null when no dbt project configured", result)
    }

    fun testFindSeedFileReturnsNullWithoutDbtProject() {
        val result = DbtDirectories.findSeedFile(project, "raw_customers")
        assertNull("Should return null when no dbt project configured", result)
    }

    fun testFindAllModelsReturnsEmptyWithoutDbtProject() {
        val result = DbtDirectories.findAllModels(project)
        assertNotNull(result)
        assertTrue("Should return empty list when no dbt project configured", result.isEmpty())
    }

    fun testFindAllSeedsReturnsEmptyWithoutDbtProject() {
        val result = DbtDirectories.findAllSeeds(project)
        assertNotNull(result)
        assertTrue("Should return empty list when no dbt project configured", result.isEmpty())
    }

    // getModelNameFromFile tests
    fun testGetModelNameFromFileExtractsName() {
        val psiFile = myFixture.configureByText("stg_customers.sql", "SELECT 1")
        val name = DbtDirectories.getModelNameFromFile(psiFile)
        assertEquals("stg_customers", name)
    }

    fun testGetModelNameFromFileWithUnderscores() {
        val psiFile = myFixture.configureByText("stg_my_model_v2.sql", "SELECT 1")
        val name = DbtDirectories.getModelNameFromFile(psiFile)
        assertEquals("stg_my_model_v2", name)
    }

    fun testGetModelNameFromFileWithDashes() {
        val psiFile = myFixture.configureByText("my-model-name.sql", "SELECT 1")
        val name = DbtDirectories.getModelNameFromFile(psiFile)
        assertEquals("my-model-name", name)
    }

    // getSeedNameFromFile tests
    fun testGetSeedNameFromFileExtractsName() {
        val psiFile = myFixture.configureByText("raw_customers.csv", "id,name\n1,test")
        val name = DbtDirectories.getSeedNameFromFile(psiFile.virtualFile)
        assertEquals("raw_customers", name)
    }

    fun testGetSeedNameFromFileWithUnderscores() {
        val psiFile = myFixture.configureByText("raw_data_v2.csv", "id\n1")
        val name = DbtDirectories.getSeedNameFromFile(psiFile.virtualFile)
        assertEquals("raw_data_v2", name)
    }

    // Edge cases - methods don't throw on various inputs
    fun testFindModelDoesNotThrowWithEmptyName() {
        assertDoesNotThrow {
            DbtDirectories.findModel(project, "")
        }
    }

    fun testFindModelDoesNotThrowWithSpecialCharacters() {
        assertDoesNotThrow {
            DbtDirectories.findModel(project, "model-with-dashes_and_underscores")
        }
    }

    fun testFindSeedFileDoesNotThrowWithEmptyName() {
        assertDoesNotThrow {
            DbtDirectories.findSeedFile(project, "")
        }
    }

    fun testFindAllModelsDoesNotThrow() {
        assertDoesNotThrow {
            DbtDirectories.findAllModels(project)
        }
    }

    fun testFindAllSeedsDoesNotThrow() {
        assertDoesNotThrow {
            DbtDirectories.findAllSeeds(project)
        }
    }

    // Multiple calls tests
    fun testMultipleCallsToFindAllModels() {
        val result1 = DbtDirectories.findAllModels(project)
        val result2 = DbtDirectories.findAllModels(project)

        assertEquals(result1.size, result2.size)
    }

    fun testMultipleCallsToFindAllSeeds() {
        val result1 = DbtDirectories.findAllSeeds(project)
        val result2 = DbtDirectories.findAllSeeds(project)

        assertEquals(result1.size, result2.size)
    }

    // Test that searching for nonexistent models returns null
    fun testFindModelWithNonexistentModel() {
        val result = DbtDirectories.findModel(project, "this_model_does_not_exist_anywhere")
        assertNull(result)
    }

    fun testFindSeedWithNonexistentSeed() {
        val result = DbtDirectories.findSeedFile(project, "this_seed_does_not_exist")
        assertNull(result)
    }

    private fun assertDoesNotThrow(block: () -> Unit) {
        try {
            block()
        } catch (e: Throwable) {
            fail("Expected no exception but got: ${e.message}")
        }
    }
}

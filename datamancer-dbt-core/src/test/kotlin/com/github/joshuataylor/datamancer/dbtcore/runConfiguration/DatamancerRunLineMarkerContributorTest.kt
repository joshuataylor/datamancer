package com.github.joshuataylor.datamancer.dbtcore.runConfiguration

import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for DatamancerRunLineMarkerContributor.
 * Verifies that run gutter icons only appear on dbt model files.
 */
class DatamancerRunLineMarkerContributorTest : BasePlatformTestCase() {

    private val contributor = DatamancerRunLineMarkerContributor()

    fun testContributorCanBeInstantiated() {
        assertNotNull(DatamancerRunLineMarkerContributor())
    }

    fun testContributorExtendsRunLineMarkerContributor() {
        assertTrue(
            "Should extend RunLineMarkerContributor",
            contributor is RunLineMarkerContributor
        )
    }

    fun testGetInfoReturnsNullForNonSqlFile() {
        val psiFile = myFixture.configureByText("test.txt", "some text content")
        val firstElement = PsiTreeUtil.getDeepestFirst(psiFile)
        assertNull(
            "Should return null for non-SQL files",
            contributor.getInfo(firstElement)
        )
    }

    fun testGetInfoReturnsNullForYamlFile() {
        val psiFile = myFixture.configureByText("schema.yml", "name: test")
        val firstElement = PsiTreeUtil.getDeepestFirst(psiFile)
        assertNull(
            "Should return null for YAML files",
            contributor.getInfo(firstElement)
        )
    }

    fun testGetInfoReturnsNullForKotlinFile() {
        val psiFile = myFixture.configureByText("Test.kt", "fun main() {}")
        val firstElement = PsiTreeUtil.getDeepestFirst(psiFile)
        assertNull(
            "Should return null for Kotlin files",
            contributor.getInfo(firstElement)
        )
    }

    fun testGetInfoReturnsNullForSqlFileNotInModelsDirectory() {
        // Files created via addFileToProject get a path in the temp project dir
        val psiFile = myFixture.addFileToProject("macros/my_macro.sql", "SELECT 1")
        val firstElement = PsiTreeUtil.getDeepestFirst(psiFile)
        assertNull(
            "Should return null for SQL files not in models/ directory",
            contributor.getInfo(firstElement)
        )
    }

    fun testGetInfoReturnsNullForSqlInSeedsDirectory() {
        val psiFile = myFixture.addFileToProject("seeds/my_seed.sql", "SELECT 1")
        val firstElement = PsiTreeUtil.getDeepestFirst(psiFile)
        assertNull(
            "Should return null for SQL files in seeds/ directory",
            contributor.getInfo(firstElement)
        )
    }

    fun testGetInfoReturnsNullForModelFileWithoutDbtConfig() {
        // Even in models/ dir, without a dbt config on the module it should return null
        val psiFile = myFixture.addFileToProject("models/my_model.sql", "SELECT 1")
        val firstElement = PsiTreeUtil.getDeepestFirst(psiFile)
        assertNull(
            "Should return null when module has no dbt project configuration",
            contributor.getInfo(firstElement)
        )
    }

    fun testGetInfoMethodExists() {
        val method = DatamancerRunLineMarkerContributor::class.java.methods.find { it.name == "getInfo" }
        assertNotNull("getInfo method should exist", method)
    }
}

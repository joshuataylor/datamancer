package com.github.joshuataylor.datamancer.core.services

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for DbtVarIndexService.
 * Tests variable indexing and lookup from dbt_project.yml files.
 *
 * Note: Full integration tests with actual dbt_project.yml files would require
 * setting up a dbt project with the DatamancerDbtProjectIndexService configured.
 */
class DbtVarIndexServiceTest : BasePlatformTestCase() {

    // Service instantiation tests
    fun testServiceInstanceCreation() {
        val service = DbtVarIndexService.getInstance(project)
        assertNotNull(service)
    }

    fun testServiceIsSingleton() {
        val service1 = DbtVarIndexService.getInstance(project)
        val service2 = DbtVarIndexService.getInstance(project)
        assertSame(service1, service2)
    }

    fun testMultipleCallsToGetInstance() {
        val instances = (1..5).map {
            DbtVarIndexService.getInstance(project)
        }

        val first = instances.first()
        instances.forEach { instance ->
            assertSame(first, instance)
        }
    }

    fun testServiceExistsInProject() {
        val service = project.getService(DbtVarIndexService::class.java)
        assertNotNull(service)
    }

    // Method existence tests
    fun testGetAllVarsMethodExists() {
        val service = DbtVarIndexService.getInstance(project)
        val method = service::class.java.methods.find { it.name == "getAllVars" }
        assertNotNull("getAllVars method should exist", method)
    }

    fun testFindVarMethodExists() {
        val service = DbtVarIndexService.getInstance(project)
        val method = service::class.java.methods.find { it.name == "findVar" }
        assertNotNull("findVar method should exist", method)
    }

    fun testGetAllVarNamesMethodExists() {
        val service = DbtVarIndexService.getInstance(project)
        val method = service::class.java.methods.find { it.name == "getAllVarNames" }
        assertNotNull("getAllVarNames method should exist", method)
    }

    // Empty project tests
    fun testGetAllVarsWithNoVars() {
        val service = DbtVarIndexService.getInstance(project)
        val vars = service.getAllVars()
        assertNotNull(vars)
        assertTrue("Should return empty map when no vars", vars.isEmpty())
    }

    fun testFindVarByNameNotFound() {
        val service = DbtVarIndexService.getInstance(project)
        val varDef = service.findVar("nonexistent_var")
        assertNull("Should return null for nonexistent var", varDef)
    }

    fun testGetAllVarNamesEmpty() {
        val service = DbtVarIndexService.getInstance(project)
        val names = service.getAllVarNames()
        assertNotNull(names)
        assertTrue("Should return empty list when no vars", names.isEmpty())
    }

    // Data class tests
    fun testVarDefinitionDataClass() {
        val clazz = DbtVarIndexService.VarDefinition::class
        assertNotNull(clazz)

        val constructor = clazz.constructors.firstOrNull()
        assertNotNull("VarDefinition should have a constructor", constructor)

        val paramNames = constructor?.parameters?.map { it.name }
        assertTrue("Should have 'name' parameter", paramNames?.contains("name") == true)
        assertTrue("Should have 'value' parameter", paramNames?.contains("value") == true)
        assertTrue("Should have 'psiElement' parameter", paramNames?.contains("psiElement") == true)
        assertTrue("Should have 'file' parameter", paramNames?.contains("file") == true)
        assertTrue("Should have 'scope' parameter", paramNames?.contains("scope") == true)
    }

    // Edge cases
    fun testFindVarWithEmptyName() {
        val service = DbtVarIndexService.getInstance(project)
        val varDef = service.findVar("")
        assertNull("Should return null for empty var name", varDef)
    }

    // Service doesn't throw on invalid input
    fun testServiceHandlesGracefully() {
        val service = DbtVarIndexService.getInstance(project)

        // These should not throw
        assertDoesNotThrow { service.getAllVars() }
        assertDoesNotThrow { service.getAllVarNames() }
        assertDoesNotThrow { service.findVar("any") }
        assertDoesNotThrow { service.findVar("") }
    }

    private fun assertDoesNotThrow(block: () -> Unit) {
        try {
            block()
        } catch (e: Throwable) {
            fail("Expected no exception but got: ${e.message}")
        }
    }
}

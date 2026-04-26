package com.github.joshuataylor.datamancer.core.services

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for DbtSourceIndexService.
 * Tests source indexing and lookup from schema.yml files.
 *
 * Note: Full integration tests with actual schema.yml files would require
 * setting up a dbt project with the DatamancerDbtProjectIndexService configured.
 */
class DbtSourceIndexServiceTest : BasePlatformTestCase() {

    // Service instantiation tests
    fun testServiceInstanceCreation() {
        val service = DbtSourceIndexService.getInstance(project)
        assertNotNull(service)
    }

    fun testServiceIsSingleton() {
        val service1 = DbtSourceIndexService.getInstance(project)
        val service2 = DbtSourceIndexService.getInstance(project)
        assertSame(service1, service2)
    }

    fun testMultipleCallsToGetInstance() {
        val instances = (1..5).map {
            DbtSourceIndexService.getInstance(project)
        }

        val first = instances.first()
        instances.forEach { instance ->
            assertSame(first, instance)
        }
    }

    fun testServiceExistsInProject() {
        val service = project.getService(DbtSourceIndexService::class.java)
        assertNotNull(service)
    }

    // Method existence tests
    fun testGetAllSourcesMethodExists() {
        val service = DbtSourceIndexService.getInstance(project)
        val method = service::class.java.methods.find { it.name == "getAllSources" }
        assertNotNull("getAllSources method should exist", method)
    }

    fun testFindSourceMethodExists() {
        val service = DbtSourceIndexService.getInstance(project)
        val method = service::class.java.methods.find { it.name == "findSource" }
        assertNotNull("findSource method should exist", method)
    }

    fun testFindSourceTableMethodExists() {
        val service = DbtSourceIndexService.getInstance(project)
        val method = service::class.java.methods.find { it.name == "findSourceTable" }
        assertNotNull("findSourceTable method should exist", method)
    }

    fun testGetAllSourceNamesMethodExists() {
        val service = DbtSourceIndexService.getInstance(project)
        val method = service::class.java.methods.find { it.name == "getAllSourceNames" }
        assertNotNull("getAllSourceNames method should exist", method)
    }

    fun testGetTableNamesMethodExists() {
        val service = DbtSourceIndexService.getInstance(project)
        val method = service::class.java.methods.find { it.name == "getTableNames" }
        assertNotNull("getTableNames method should exist", method)
    }

    // Empty project tests
    fun testGetAllSourcesWithNoSources() {
        val service = DbtSourceIndexService.getInstance(project)
        val sources = service.getAllSources()
        assertNotNull(sources)
        assertTrue("Should return empty map when no sources", sources.isEmpty())
    }

    fun testFindSourceByNameNotFound() {
        val service = DbtSourceIndexService.getInstance(project)
        val source = service.findSource("nonexistent_source")
        assertNull("Should return null for nonexistent source", source)
    }

    fun testFindSourceTableNotFound() {
        val service = DbtSourceIndexService.getInstance(project)
        val table = service.findSourceTable("nonexistent_source", "nonexistent_table")
        assertNull("Should return null for nonexistent source/table", table)
    }

    fun testGetAllSourceNamesEmpty() {
        val service = DbtSourceIndexService.getInstance(project)
        val names = service.getAllSourceNames()
        assertNotNull(names)
        assertTrue("Should return empty list when no sources", names.isEmpty())
    }

    fun testGetTableNamesForNonexistentSource() {
        val service = DbtSourceIndexService.getInstance(project)
        val tables = service.getTableNames("nonexistent_source")
        assertNotNull(tables)
        assertTrue("Should return empty list for nonexistent source", tables.isEmpty())
    }

    // Data class tests
    fun testSourceDefinitionDataClass() {
        // Verify the data class exists and has expected properties
        val clazz = DbtSourceIndexService.SourceDefinition::class
        assertNotNull(clazz)

        val constructor = clazz.constructors.firstOrNull()
        assertNotNull("SourceDefinition should have a constructor", constructor)

        // Check parameter names
        val paramNames = constructor?.parameters?.map { it.name }
        assertTrue("Should have 'name' parameter", paramNames?.contains("name") == true)
        assertTrue("Should have 'tables' parameter", paramNames?.contains("tables") == true)
        assertTrue("Should have 'psiElement' parameter", paramNames?.contains("psiElement") == true)
        assertTrue("Should have 'file' parameter", paramNames?.contains("file") == true)
    }

    fun testTableDefinitionDataClass() {
        val clazz = DbtSourceIndexService.TableDefinition::class
        assertNotNull(clazz)

        val constructor = clazz.constructors.firstOrNull()
        assertNotNull("TableDefinition should have a constructor", constructor)

        val paramNames = constructor?.parameters?.map { it.name }
        assertTrue("Should have 'name' parameter", paramNames?.contains("name") == true)
        assertTrue("Should have 'psiElement' parameter", paramNames?.contains("psiElement") == true)
    }

    // Edge cases
    fun testFindSourceWithEmptyName() {
        val service = DbtSourceIndexService.getInstance(project)
        val source = service.findSource("")
        assertNull("Should return null for empty source name", source)
    }

    fun testFindSourceTableWithEmptyNames() {
        val service = DbtSourceIndexService.getInstance(project)
        val table = service.findSourceTable("", "")
        assertNull("Should return null for empty names", table)
    }

    fun testGetTableNamesWithEmptySource() {
        val service = DbtSourceIndexService.getInstance(project)
        val tables = service.getTableNames("")
        assertNotNull(tables)
        assertTrue("Should return empty list for empty source name", tables.isEmpty())
    }

    // Service doesn't throw on invalid input
    fun testServiceHandlesNullGracefully() {
        val service = DbtSourceIndexService.getInstance(project)

        // These should not throw
        assertDoesNotThrow { service.getAllSources() }
        assertDoesNotThrow { service.getAllSourceNames() }
        assertDoesNotThrow { service.findSource("any") }
        assertDoesNotThrow { service.findSourceTable("any", "any") }
        assertDoesNotThrow { service.getTableNames("any") }
    }

    private fun assertDoesNotThrow(block: () -> Unit) {
        try {
            block()
        } catch (e: Throwable) {
            fail("Expected no exception but got: ${e.message}")
        }
    }
}

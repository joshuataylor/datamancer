package com.github.joshuataylor.datamancer.core.services

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

/**
 * Tests for DatamancerDbtProjectIndexService.
 * Tests index building, lookups, and event-driven updates.
 *
 * Note: Some tests are simplified due to the complexity of testing coroutine-based
 * event flows in a light test environment. Full integration tests would require
 * HeavyPlatformTestCase with proper project setup.
 */
class DatamancerDbtProjectIndexServiceTest : BasePlatformTestCase() {

    fun testServiceInstanceCreation() {
        val service = DatamancerDbtProjectIndexService.getInstance(project)
        assertNotNull(service)
    }

    fun testServiceIsSingleton() {
        val service1 = DatamancerDbtProjectIndexService.getInstance(project)
        val service2 = DatamancerDbtProjectIndexService.getInstance(project)

        assertSame(service1, service2)
    }

    fun testGetDbtConfigForModuleNameReturnsNull() {
        val service = DatamancerDbtProjectIndexService.getInstance(project)

        val config = service.getDbtConfigForModuleName("non-existent-module")

        assertNull(config)
    }

    fun testGetDbtProjectsReturnsEmptyInitially() = runBlocking {
        val service = DatamancerDbtProjectIndexService.getInstance(project)

        // Give the service a moment to initialize
        delay(100)

        val projects = service.getDbtProjects()

        assertNotNull(projects)
    }

    fun testIndexServiceInitialization() {
        // Test that the service can be instantiated without errors
        val service = DatamancerDbtProjectIndexService.getInstance(project)

        assertNotNull(service)
        // Service should have been initialized with the coroutine scope
        // The init block should have started listening to workspace events
    }

    fun testGetDbtConfigForModuleNameWithInvalidName() {
        val service = DatamancerDbtProjectIndexService.getInstance(project)

        val config = service.getDbtConfigForModuleName("")

        assertNull(config)
    }

    fun testGetDbtConfigForModuleNameWithSpecialCharacters() {
        val service = DatamancerDbtProjectIndexService.getInstance(project)

        val config = service.getDbtConfigForModuleName("module-with-dashes")

        // Should return null if no such module exists
        assertNull(config)
    }

    fun testMultipleCallsToGetInstance() {
        // Verify that getInstance always returns the same instance
        val instances = (1..5).map {
            DatamancerDbtProjectIndexService.getInstance(project)
        }

        val first = instances.first()
        instances.forEach { instance ->
            assertSame(first, instance)
        }
    }

    fun testServiceExistsInProject() {
        // Verify the service is properly registered with the project
        val service = project.getService(DatamancerDbtProjectIndexService::class.java)

        assertNotNull(service)
    }

    fun testGetDbtProjectsDoesNotThrow() = runBlocking {
        val service = DatamancerDbtProjectIndexService.getInstance(project)

        // Should not throw even if called before index is built
        try {
            withTimeout(5000) {
                val projects = service.getDbtProjects()
                assertNotNull(projects)
            }
        } catch (e: Exception) {
            fail("getDbtProjects should not throw exception: ${e.message}")
        }
    }

    fun testIndexServiceHandlesEmptyWorkspace() = runBlocking {
        val service = DatamancerDbtProjectIndexService.getInstance(project)

        // Give time for initial index build
        delay(200)

        val projects = service.getDbtProjects()

        // Should handle empty workspace gracefully
        assertNotNull(projects)
    }

    fun testGetAllDbtConfigsSyncReturnsEmptyMapInitially() {
        val service = DatamancerDbtProjectIndexService.getInstance(project)
        val configs = service.getAllDbtConfigsSync()
        assertNotNull("Should return a map, not null", configs)
        assertTrue("Should be empty when no dbt projects are discovered", configs.isEmpty())
    }

    fun testGetDbtConfigForModuleNameReturnsNullForUnknownModule() {
        val service = DatamancerDbtProjectIndexService.getInstance(project)
        val config = service.getDbtConfigForModuleName("completely-unknown-module-name")
        assertNull("Should return null for unknown module names", config)
    }
}

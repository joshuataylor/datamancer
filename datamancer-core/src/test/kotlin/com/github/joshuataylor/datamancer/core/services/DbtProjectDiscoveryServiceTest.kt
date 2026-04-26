package com.github.joshuataylor.datamancer.core.services

import com.intellij.openapi.components.Service
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for DbtProjectDiscoveryService.
 * Tests dbt project discovery and module association.
 *
 * Note: Full integration tests with actual file discovery would require
 * HeavyPlatformTestCase with proper module and file structure setup.
 */
class DbtProjectDiscoveryServiceTest : BasePlatformTestCase() {

    fun testServiceInstanceCreation() {
        val service = DatamancerProjectDiscoveryService.getInstance(project)
        assertNotNull(service)
    }

    fun testServiceIsSingleton() {
        val service1 = DatamancerProjectDiscoveryService.getInstance(project)
        val service2 = DatamancerProjectDiscoveryService.getInstance(project)
        assertSame(service1, service2)
    }

    fun testServiceExistsInProject() {
        val service = project.getService(DatamancerProjectDiscoveryService::class.java)
        assertNotNull(service)
    }

    fun testServiceClassIsAnnotatedWithServiceAnnotation() {
        val serviceAnnotation = DatamancerProjectDiscoveryService::class.java.getAnnotation(Service::class.java)
        assertNotNull("Service should be annotated with @Service", serviceAnnotation)
    }

    fun testDiscoverAndAssociateMethodExists() {
        val method = DatamancerProjectDiscoveryService::class.java.methods.find {
            it.name == "discoverAndAssociate"
        }
        assertNotNull("discoverAndAssociate method should exist", method)
    }

    fun testFindAllDbtProjectsMethodExists() {
        val method = DatamancerProjectDiscoveryService::class.java.methods.find {
            it.name == "findAllDbtProjects"
        }
        assertNotNull("findAllDbtProjects method should exist", method)
    }

    fun testGetInstanceCompanionMethod() {
        val companionService = DatamancerProjectDiscoveryService.getInstance(project)
        val directService = project.getService(DatamancerProjectDiscoveryService::class.java)
        assertSame(
            "getInstance() and direct service retrieval should return same instance",
            companionService, directService
        )
    }
}

package com.github.joshuataylor.datamancer.dbtcore.mise

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for DatamancerMiseIntegration.
 * Verifies graceful degradation when the mise plugin is not installed.
 */
class DatamancerMiseIntegrationTest : BasePlatformTestCase() {

    fun testMisePluginNotAvailableInTestEnvironment() {
        // The mise plugin is not installed in the test sandbox
        assertFalse(
            "Mise plugin should not be available in test environment",
            DatamancerMiseIntegration.isMisePluginAvailable()
        )
    }

    fun testIsMiseEnabledReturnsFalseWhenPluginNotAvailable() {
        assertFalse(
            "Should return false when mise plugin is not installed",
            DatamancerMiseIntegration.isMiseEnabledForProject(project)
        )
    }

    fun testGetMiseEnvironmentVariablesReturnsEmptyMapWhenPluginNotAvailable() {
        val envVars = DatamancerMiseIntegration.getMiseEnvironmentVariables(
            project = project,
            workingDirectory = project.basePath ?: "/tmp",
        )
        assertNotNull(envVars)
        assertTrue(
            "Should return empty map when mise plugin is not installed",
            envVars.isEmpty()
        )
    }
}

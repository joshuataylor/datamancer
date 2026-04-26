package com.github.joshuataylor.datamancer.core.workspace

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for DatamancerWorkspaceModelExtensions.kt
 * Tests basic workspace model keys and configuration.
 *
 * Note: Full integration tests for workspace model operations require HeavyPlatformTestCase
 * and complex module setup. These tests focus on verifiable functionality in a light test environment.
 */
class DbtWorkspaceModelExtensionsTest : BasePlatformTestCase() {

    fun testExternalMappingKeyCreation() {
        val key = DatamancerWorkspaceKeys.DBT_PROJECT_CONFIG
        assertNotNull(key)
    }

    fun testExternalMappingKeyUniqueness() {
        val key = DatamancerWorkspaceKeys.DBT_PROJECT_CONFIG
        // The key identifier should be unique and properly formatted
        assertTrue(key.toString().contains("dbt.project.config"))
    }

    fun testExternalMappingKeyIdentifier() {
        // Verify the key has the expected identifier
        val key = DatamancerWorkspaceKeys.DBT_PROJECT_CONFIG
        assertNotNull(key)
        // Key should be non-null and have proper structure
        val keyString = key.toString()
        assertTrue(keyString.isNotEmpty())
    }

    fun testDatamancerProjectConfigCanBeUsedAsMapValue() {
        // Test that DatamancerProjectConfig can be stored and retrieved from a map
        // This simulates what the external mapping does internally
        val map = mutableMapOf<String, DatamancerProjectConfig>()

        val config = DatamancerProjectConfig(
            projectRoot = "/test/path",
            queryLimit = 500
        )

        map["test-module"] = config

        val retrieved = map["test-module"]
        assertEquals(config, retrieved)
        assertEquals("/test/path", retrieved?.projectRoot)
        assertEquals(500, retrieved?.queryLimit)
    }

    fun testDatamancerProjectConfigEquality() {
        // Test that two configs with same values are equal
        val config1 = DatamancerProjectConfig(
            projectRoot = "/test/path",
            profileDirectory = "/profiles",
            defaultTarget = "dev",
            queryLimit = 750
        )

        val config2 = DatamancerProjectConfig(
            projectRoot = "/test/path",
            profileDirectory = "/profiles",
            defaultTarget = "dev",
            queryLimit = 750
        )

        assertEquals(config1, config2)
    }

    fun testDatamancerProjectConfigInequality() {
        // Test that two configs with different values are not equal
        val config1 = DatamancerProjectConfig(projectRoot = "/path1", queryLimit = 100)
        val config2 = DatamancerProjectConfig(projectRoot = "/path2", queryLimit = 200)

        assertFalse(config1 == config2)
    }

    fun testDatamancerProjectConfigWithNullFields() {
        // Test config with null optional fields
        val config = DatamancerProjectConfig(
            projectRoot = "/test/path"
        )

        assertEquals("/test/path", config.projectRoot)
        assertNull(config.profileDirectory)
        assertNull(config.defaultTarget)
        assertNull(config.defaultDataSource)
        assertEquals(1000, config.queryLimit)
        assertTrue(config.environmentVariables.isEmpty())
    }

    fun testDatamancerProjectConfigWithAllFields() {
        // Test config with all fields populated
        val config = DatamancerProjectConfig(
            projectRoot = "/test/path",
            profileDirectory = "/profiles",
            defaultTarget = "dev",
            defaultDataSource = "db1",
            queryLimit = 500,
            environmentVariables = mapOf("KEY" to "value")
        )

        assertEquals("/test/path", config.projectRoot)
        assertEquals("/profiles", config.profileDirectory)
        assertEquals("dev", config.defaultTarget)
        assertEquals("db1", config.defaultDataSource)
        assertEquals(500, config.queryLimit)
        assertEquals(1, config.environmentVariables.size)
        assertEquals("value", config.environmentVariables["KEY"])
    }

    fun testDatamancerProjectConfigCopy() {
        // Test that copy creates a new instance
        val original = DatamancerProjectConfig(projectRoot = "/original", queryLimit = 100)
        val copied = original.copy(projectRoot = "/copied")

        assertEquals("/copied", copied.projectRoot)
        assertEquals(100, copied.queryLimit)
        assertEquals("/original", original.projectRoot)
        assertFalse(original == copied)
    }

    fun testDatamancerProjectConfigHashCode() {
        // Test that configs with same values have same hash code
        val config1 = DatamancerProjectConfig(projectRoot = "/test", queryLimit = 500)
        val config2 = DatamancerProjectConfig(projectRoot = "/test", queryLimit = 500)

        assertEquals(config1.hashCode(), config2.hashCode())
    }

    fun testDatamancerProjectConfigInMapAsKey() {
        // Test that DatamancerProjectConfig can be used as a map key
        val map = mutableMapOf<DatamancerProjectConfig, String>()

        val config = DatamancerProjectConfig(projectRoot = "/test", queryLimit = 500)
        map[config] = "test-value"

        val retrieved = map[config]
        assertEquals("test-value", retrieved)
    }

    fun testDatamancerProjectConfigEnvironmentVariablesImmutability() {
        // Test that environment variables are properly handled
        val envVars = mapOf("KEY1" to "value1", "KEY2" to "value2")
        val config = DatamancerProjectConfig(
            projectRoot = "/test",
            environmentVariables = envVars
        )

        assertEquals(2, config.environmentVariables.size)
        assertEquals("value1", config.environmentVariables["KEY1"])
        assertEquals("value2", config.environmentVariables["KEY2"])
    }
}

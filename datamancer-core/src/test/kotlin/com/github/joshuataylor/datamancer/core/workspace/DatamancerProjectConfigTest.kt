package com.github.joshuataylor.datamancer.core.workspace

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for DatamancerProjectConfig data class.
 * Tests default values, data class operations, and field handling.
 */
class DatamancerProjectConfigTest : BasePlatformTestCase() {

    fun testDefaultValues() {
        val config = DatamancerProjectConfig(projectRoot = "/test/path")

        assertEquals(1000, config.queryLimit)
        assertTrue(config.environmentVariables.isEmpty())
        assertNull(config.profileDirectory)
        assertNull(config.defaultTarget)
        assertNull(config.defaultDataSource)
        assertTrue(config.useMiseEnvironment)
    }

    fun testDataClassEquality() {
        val config1 = DatamancerProjectConfig(
            projectRoot = "/test/path",
            profileDirectory = "/profiles",
            defaultTarget = "dev",
            defaultDataSource = "test_db",
            queryLimit = 500,
            environmentVariables = mapOf("DBT_ENV" to "test", "DBT_PROFILES_DIR" to "/custom")
        )

        val config2 = DatamancerProjectConfig(
            projectRoot = "/test/path",
            profileDirectory = "/profiles",
            defaultTarget = "dev",
            defaultDataSource = "test_db",
            queryLimit = 500,
            environmentVariables = mapOf("DBT_ENV" to "test", "DBT_PROFILES_DIR" to "/custom")
        )

        assertEquals(config1, config2)
        assertEquals(config1.hashCode(), config2.hashCode())
    }

    fun testCopyWithModifications() {
        val original = DatamancerProjectConfig(
            projectRoot = "/test/path",
            queryLimit = 1000
        )

        val modified = original.copy(
            queryLimit = 500,
            defaultTarget = "prod"
        )

        assertEquals("/test/path", modified.projectRoot)
        assertEquals(500, modified.queryLimit)
        assertEquals("prod", modified.defaultTarget)
        assertNull(modified.profileDirectory)
        assertNull(modified.defaultDataSource)
        assertTrue(modified.environmentVariables.isEmpty())
        assertTrue(modified.useMiseEnvironment)
    }

    fun testUseMiseEnvironmentCanBeDisabled() {
        val config = DatamancerProjectConfig(
            projectRoot = "/test/path",
            useMiseEnvironment = false
        )
        assertFalse(config.useMiseEnvironment)

        val copied = config.copy(useMiseEnvironment = true)
        assertTrue(copied.useMiseEnvironment)
    }

    fun testNullableFieldHandling() {
        // Test with all nullable fields as null
        val configAllNull = DatamancerProjectConfig(projectRoot = "/test/path")
        assertNull(configAllNull.profileDirectory)
        assertNull(configAllNull.defaultTarget)
        assertNull(configAllNull.defaultDataSource)

        // Test with all nullable fields set
        val configAllSet = DatamancerProjectConfig(
            projectRoot = "/test/path",
            profileDirectory = "/profiles",
            defaultTarget = "dev",
            defaultDataSource = "my_db"
        )
        assertEquals("/profiles", configAllSet.profileDirectory)
        assertEquals("dev", configAllSet.defaultTarget)
        assertEquals("my_db", configAllSet.defaultDataSource)
    }

    fun testEnvironmentVariablesHandling() {
        // Test with empty map
        val configEmpty = DatamancerProjectConfig(
            projectRoot = "/test/path",
            environmentVariables = emptyMap()
        )
        assertTrue(configEmpty.environmentVariables.isEmpty())

        // Test with populated map
        val envVars = mapOf("KEY1" to "value1", "KEY2" to "value2")
        val config = DatamancerProjectConfig(
            projectRoot = "/test/path",
            environmentVariables = envVars
        )

        // Verify environment variables are stored
        assertEquals(2, config.environmentVariables.size)
        assertEquals("value1", config.environmentVariables["KEY1"])
        assertEquals("value2", config.environmentVariables["KEY2"])

        // Test copy preserves environment variables
        val copied = config.copy()
        assertEquals(config.environmentVariables, copied.environmentVariables)
    }
}

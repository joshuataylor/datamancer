package com.github.joshuataylor.datamancer.dbtcore.runConfiguration

import com.intellij.execution.configurations.RunConfigurationOptions
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for DatamancerRunConfigurationOptions.
 * Verifies property storage, defaults, and the queryLimit string-to-int conversion.
 */
class DatamancerRunConfigurationOptionsTest : BasePlatformTestCase() {

    private fun createOptions() = DatamancerRunConfigurationOptions()

    // Instantiation
    fun testOptionsCanBeInstantiated() {
        assertNotNull(createOptions())
    }

    fun testOptionsExtendsRunConfigurationOptions() {
        assertTrue(
            "Should extend RunConfigurationOptions",
            createOptions() is RunConfigurationOptions
        )
    }

    // Default values
    fun testModelNameDefaultsToEmptyString() {
        assertEquals("", createOptions().modelName)
    }

    fun testModuleNameDefaultsToEmptyString() {
        assertEquals("", createOptions().moduleName)
    }

    fun testDataSourceNameDefaultsToEmptyString() {
        assertEquals("", createOptions().dataSourceName)
    }

    fun testQueryLimitDefaultsToNull() {
        // Stored as empty string, toIntOrNull() returns null
        assertNull(createOptions().queryLimit)
    }

    fun testEnvironmentVariablesDefaultsToEmptyMap() {
        val envVars = createOptions().environmentVariables
        assertNotNull(envVars)
        assertTrue("Should default to empty map", envVars.isEmpty())
    }

    // Setter/getter round-trip
    fun testModelNameSetAndGet() {
        val options = createOptions()
        options.modelName = "stg_customers"
        assertEquals("stg_customers", options.modelName)
    }

    fun testModuleNameSetAndGet() {
        val options = createOptions()
        options.moduleName = "my-module"
        assertEquals("my-module", options.moduleName)
    }

    fun testDataSourceNameSetAndGet() {
        val options = createOptions()
        options.dataSourceName = "my_database"
        assertEquals("my_database", options.dataSourceName)
    }

    fun testQueryLimitSetAndGet() {
        val options = createOptions()
        options.queryLimit = 500
        assertEquals(500, options.queryLimit)
    }

    fun testQueryLimitSetToNull() {
        val options = createOptions()
        options.queryLimit = 100
        options.queryLimit = null
        assertNull(options.queryLimit)
    }

    fun testQueryLimitSetToZero() {
        val options = createOptions()
        options.queryLimit = 0
        assertEquals(0, options.queryLimit)
    }

    fun testEnvironmentVariablesSetAndGet() {
        val options = createOptions()
        options.environmentVariables = mutableMapOf("DBT_TARGET" to "dev", "DBT_PROFILES_DIR" to "/tmp")
        assertEquals("dev", options.environmentVariables["DBT_TARGET"])
        assertEquals("/tmp", options.environmentVariables["DBT_PROFILES_DIR"])
        assertEquals(2, options.environmentVariables.size)
    }

    fun testUseMiseEnvironmentDefaultsToTrue() {
        assertTrue(
            "useMiseEnvironment should default to true",
            createOptions().useMiseEnvironment
        )
    }

    fun testUseMiseEnvironmentSetAndGet() {
        val options = createOptions()
        options.useMiseEnvironment = false
        assertFalse(options.useMiseEnvironment)
    }

    fun testUseMiseEnvironmentRoundTrip() {
        val options = createOptions()
        assertTrue(options.useMiseEnvironment)
        options.useMiseEnvironment = false
        assertFalse(options.useMiseEnvironment)
        options.useMiseEnvironment = true
        assertTrue(options.useMiseEnvironment)
    }

    // Edge cases
    fun testModelNameSetToNull() {
        val options = createOptions()
        options.modelName = null
        assertNull(options.modelName)
    }

    fun testQueryLimitNegativeValue() {
        val options = createOptions()
        options.queryLimit = -1
        assertEquals(-1, options.queryLimit)
    }

    fun testQueryLimitLargeValue() {
        val options = createOptions()
        options.queryLimit = Int.MAX_VALUE
        assertEquals(Int.MAX_VALUE, options.queryLimit)
    }
}

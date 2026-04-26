package com.github.joshuataylor.datamancer.dbtcore.runConfiguration

import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for DatamancerRunConfiguration.
 * Verifies property delegation to options and basic configuration behaviour.
 */
class DatamancerRunConfigurationTest : BasePlatformTestCase() {

    private fun createRunConfiguration(name: String = "Test Config"): DatamancerRunConfiguration {
        val type = DatamancerRunConfigurationType()
        val factory = type.configurationFactories.first()
        return DatamancerRunConfiguration(project, factory, name)
    }

    fun testRunConfigurationCanBeCreated() {
        assertNotNull(createRunConfiguration())
    }

    fun testRunConfigurationExtendsRunConfigurationBase() {
        assertTrue(
            "Should extend RunConfigurationBase",
            createRunConfiguration() is RunConfigurationBase<*>
        )
    }

    fun testConfigurationName() {
        assertEquals("Test Config", createRunConfiguration().name)
    }

    fun testCustomConfigurationName() {
        assertEquals("dbt: my_model", createRunConfiguration("dbt: my_model").name)
    }

    // Property delegation tests
    fun testModelNameDelegatesToOptions() {
        val config = createRunConfiguration()
        config.modelName = "stg_customers"
        assertEquals("stg_customers", config.modelName)
    }

    fun testModuleNameDelegatesToOptions() {
        val config = createRunConfiguration()
        config.moduleName = "my-module"
        assertEquals("my-module", config.moduleName)
    }

    fun testDataSourceNameDelegatesToOptions() {
        val config = createRunConfiguration()
        config.dataSourceName = "production_db"
        assertEquals("production_db", config.dataSourceName)
    }

    fun testQueryLimitDelegatesToOptions() {
        val config = createRunConfiguration()
        config.queryLimit = 1000
        assertEquals(1000, config.queryLimit)
    }

    fun testQueryLimitDefaultsToNull() {
        assertNull(createRunConfiguration().queryLimit)
    }

    fun testEnvironmentVariablesDelegatesToOptions() {
        val config = createRunConfiguration()
        config.environmentVariables = mutableMapOf("KEY" to "VALUE")
        assertEquals("VALUE", config.environmentVariables["KEY"])
    }

    fun testUseMiseEnvironmentDelegatesToOptions() {
        val config = createRunConfiguration()
        assertTrue("Should default to true", config.useMiseEnvironment)
        config.useMiseEnvironment = false
        assertFalse(config.useMiseEnvironment)
    }

    fun testGetConfigurationEditorMethodExists() {
        val method = DatamancerRunConfiguration::class.java.methods.find {
            it.name == "getConfigurationEditor"
        }
        assertNotNull("getConfigurationEditor method should exist", method)
    }

    fun testGetStateMethodExists() {
        val method = DatamancerRunConfiguration::class.java.methods.find { it.name == "getState" }
        assertNotNull("getState method should exist", method)
    }
}

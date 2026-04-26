package com.github.joshuataylor.datamancer.dbtcore.runConfiguration

import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for DatamancerRunConfigurationType.
 * Verifies the run configuration type registration and factory setup.
 */
class DatamancerRunConfigurationTypeTest : BasePlatformTestCase() {

    fun testTypeCanBeInstantiated() {
        assertNotNull(DatamancerRunConfigurationType())
    }

    fun testTypeExtendsConfigurationTypeBase() {
        assertTrue(
            "Should extend ConfigurationTypeBase",
            DatamancerRunConfigurationType() is ConfigurationTypeBase
        )
    }

    fun testTypeId() {
        assertEquals("DatamancerRunConfiguration", DatamancerRunConfigurationType.ID)
    }

    fun testTypeDisplayName() {
        val type = DatamancerRunConfigurationType()
        assertEquals("dbt Compile & Run", type.displayName)
    }

    fun testTypeDescription() {
        val type = DatamancerRunConfigurationType()
        assertEquals(
            "Compile a dbt model and execute the SQL in the database console",
            type.configurationTypeDescription
        )
    }

    fun testTypeHasOneFactory() {
        val type = DatamancerRunConfigurationType()
        assertEquals(1, type.configurationFactories.size)
    }

    fun testFactoryCreatesCorrectConfigurationType() {
        val type = DatamancerRunConfigurationType()
        val factory = type.configurationFactories.first()
        assertTrue(
            "Factory should be DatamancerConfigurationFactory",
            factory is DatamancerConfigurationFactory
        )
    }

    fun testTypeHasIcon() {
        val type = DatamancerRunConfigurationType()
        assertNotNull("Type should have an icon", type.icon)
    }
}

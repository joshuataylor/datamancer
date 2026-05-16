package com.github.joshuataylor.datamancer.dbtcore.runConfiguration

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for DatamancerConfigurationFactory.
 * Verifies configuration creation and options class mapping.
 */
class DatamancerConfigurationFactoryTest : BasePlatformTestCase() {

    private fun createFactory(): DatamancerConfigurationFactory {
        val type = DatamancerRunConfigurationType()
        return type.configurationFactories.first() as DatamancerConfigurationFactory
    }

    fun testFactoryCanBeCreated() {
        assertNotNull(createFactory())
    }

    fun testFactoryExtendsConfigurationFactory() {
        assertTrue(
            "Should extend ConfigurationFactory",
            createFactory() is ConfigurationFactory
        )
    }

    fun testFactoryIdMatchesTypeId() {
        assertEquals(DatamancerRunConfigurationType.ID, createFactory().id)
    }

    fun testCreateTemplateConfigurationReturnsRunConfiguration() {
        val config = createFactory().createTemplateConfiguration(project)
        assertTrue(
            "Should create DatamancerRunConfiguration",
            config is DatamancerRunConfiguration
        )
    }

    fun testGetOptionsClassReturnsCorrectClass() {
        assertSame(DatamancerRunConfigurationOptions::class.java, createFactory().optionsClass)
    }

    fun testCreateTemplateConfigurationHasDefaultName() {
        val config = createFactory().createTemplateConfiguration(project)
        assertEquals("dbt Compile & Run", config.name)
    }
}

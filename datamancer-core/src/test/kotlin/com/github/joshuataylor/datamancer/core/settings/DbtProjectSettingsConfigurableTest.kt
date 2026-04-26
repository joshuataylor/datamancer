package com.github.joshuataylor.datamancer.core.settings

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for ProjectSettingsConfigurable.
 * Tests settings UI configuration and management.
 *
 * Note: Full integration testing of Configurable UI requires EDT and complex
 * UI setup. These tests focus on verifiable class structure and instantiation.
 */
class DbtProjectSettingsConfigurableTest : BasePlatformTestCase() {

    fun testConfigurableCanBeInstantiated() {
        val configurable = ProjectSettingsConfigurable(project)
        assertNotNull(configurable)
    }

    fun testConfigurableIsBoundSearchableConfigurable() {
        val configurable = ProjectSettingsConfigurable(project)
        assertTrue(configurable is com.intellij.openapi.options.BoundSearchableConfigurable)
    }

    fun testConfigurableIsConfigurable() {
        val configurable = ProjectSettingsConfigurable(project)
        assertTrue(configurable is com.intellij.openapi.options.Configurable)
    }

    fun testConfigurableHasDisplayName() {
        val configurable = ProjectSettingsConfigurable(project)
        val displayName = configurable.displayName

        assertNotNull(displayName)
        assertEquals("dbt Projects", displayName)
    }

    fun testConfigurableHasId() {
        val configurable = ProjectSettingsConfigurable(project)
        val id = configurable.id

        assertNotNull(id)
        assertEquals("com.github.joshuataylor.datamancer.core.settings", id)
    }

    fun testMultipleInstancesCanBeCreated() {
        val configurable1 = ProjectSettingsConfigurable(project)
        val configurable2 = ProjectSettingsConfigurable(project)

        assertNotNull(configurable1)
        assertNotNull(configurable2)
        // Each instance should be separate
        assertNotSame(configurable1, configurable2)
    }

    fun testConfigurableIsSearchable() {
        val configurable = ProjectSettingsConfigurable(project)
        assertTrue(configurable is com.intellij.openapi.options.SearchableConfigurable)
    }

    fun testConfigurableHelpTopicIsSet() {
        val configurable = ProjectSettingsConfigurable(project)
        val helpTopic = configurable.helpTopic

        assertNotNull(helpTopic)
        assertEquals("reference.settings.dbt", helpTopic)
    }

    fun testConfigurableCanCreatePanel() {
        val configurable = ProjectSettingsConfigurable(project)

        // Verify createPanel method exists and can be called
        val createPanelMethod = configurable::class.java.methods.find {
            it.name == "createPanel" && it.parameterCount == 0
        }
        assertNotNull(createPanelMethod)
    }

    fun testConfigurableHasApplyMethod() {
        val configurable = ProjectSettingsConfigurable(project)

        val applyMethod = configurable::class.java.methods.find {
            it.name == "apply" && it.parameterCount == 0
        }
        assertNotNull(applyMethod)
    }

    fun testConfigurableHasResetMethod() {
        val configurable = ProjectSettingsConfigurable(project)

        val resetMethod = configurable::class.java.methods.find {
            it.name == "reset" && it.parameterCount == 0
        }
        assertNotNull(resetMethod)
    }

    fun testConfigurableHasIsModifiedMethod() {
        val configurable = ProjectSettingsConfigurable(project)

        val isModifiedMethod = configurable::class.java.methods.find {
            it.name == "isModified" && it.parameterCount == 0
        }
        assertNotNull(isModifiedMethod)
    }

    fun testConfigurableIdIsCorrect() {
        val configurable = ProjectSettingsConfigurable(project)

        assertEquals("com.github.joshuataylor.datamancer.core.settings", configurable.id)
    }

    fun testConfigurableInstancesHaveSameId() {
        val configurable1 = ProjectSettingsConfigurable(project)
        val configurable2 = ProjectSettingsConfigurable(project)

        assertEquals(configurable1.id, configurable2.id)
    }
}

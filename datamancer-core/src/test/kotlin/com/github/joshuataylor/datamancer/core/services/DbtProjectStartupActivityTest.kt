package com.github.joshuataylor.datamancer.core.services

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for DatamancerProjectStartupActivity.
 * Tests startup activity initialization and execution.
 *
 * Note: Full integration testing of ProjectActivity requires complex project lifecycle
 * management. These tests focus on verifiable class structure and instantiation.
 */
class DbtProjectStartupActivityTest : BasePlatformTestCase() {

    fun testStartupActivityCanBeInstantiated() {
        val activity = DatamancerProjectStartupActivity()
        assertNotNull(activity)
    }

    fun testStartupActivityIsProjectActivity() {
        val activity = DatamancerProjectStartupActivity()
        assertTrue(activity is com.intellij.openapi.startup.ProjectActivity)
    }

    fun testMultipleInstancesCanBeCreated() {
        val activity1 = DatamancerProjectStartupActivity()
        val activity2 = DatamancerProjectStartupActivity()

        assertNotNull(activity1)
        assertNotNull(activity2)
        // Each instance should be separate (not a singleton)
        assertNotSame(activity1, activity2)
    }

    fun testStartupActivityImplementsCorrectInterface() {
        val activity = DatamancerProjectStartupActivity()

        // Verify it implements ProjectActivity interface
        assertTrue(activity is com.intellij.openapi.startup.ProjectActivity)

        // Should have an execute method
        val executeMethod = activity::class.java.methods.find { it.name == "execute" }
        assertNotNull(executeMethod)
    }
}

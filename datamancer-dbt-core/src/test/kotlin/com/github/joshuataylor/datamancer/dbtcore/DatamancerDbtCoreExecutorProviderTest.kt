package com.github.joshuataylor.datamancer.dbtcore

import com.github.joshuataylor.datamancer.core.api.DatamancerDbtExecutorProvider
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for DatamancerDbtCoreExecutorProvider.
 * Verifies the dbt-core query execution backend contract.
 */
class DatamancerDbtCoreExecutorProviderTest : BasePlatformTestCase() {

    private fun createProvider() = DatamancerDbtCoreExecutorProvider()

    fun testProviderCanBeInstantiated() {
        assertNotNull(createProvider())
    }

    fun testProviderImplementsExecutorProviderInterface() {
        assertTrue(
            "Should implement DatamancerDbtExecutorProvider",
            createProvider() is DatamancerDbtExecutorProvider
        )
    }

    fun testGetIdReturnsDbtCore() {
        assertEquals("dbt-core", createProvider().getId())
    }

    fun testGetDisplayNameReturnsDbtCore() {
        assertEquals("dbt Core", createProvider().getDisplayName())
    }

    fun testIdMatchesCompilerProviderId() {
        val executorId = createProvider().getId()
        val compilerId = DatamancerDbtCoreCompilerProvider().getId()
        assertEquals(
            "Executor and compiler IDs must match for pairing",
            compilerId, executorId
        )
    }

    fun testDisplayNameMatchesCompilerProviderDisplayName() {
        val executorName = createProvider().getDisplayName()
        val compilerName = DatamancerDbtCoreCompilerProvider().getDisplayName()
        assertEquals(compilerName, executorName)
    }
}

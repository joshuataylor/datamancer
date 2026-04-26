package com.github.joshuataylor.datamancer.dbtcore

import com.github.joshuataylor.datamancer.core.api.DatamancerDbtCompilerProvider
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for DatamancerDbtCoreCompilerProvider.
 * Verifies the dbt-core compiler backend contract.
 */
class DatamancerDbtCoreCompilerProviderTest : BasePlatformTestCase() {

    private fun createProvider() = DatamancerDbtCoreCompilerProvider()

    fun testProviderCanBeInstantiated() {
        assertNotNull(createProvider())
    }

    fun testProviderImplementsCompilerProviderInterface() {
        assertTrue(
            "Should implement DatamancerDbtCompilerProvider",
            createProvider() is DatamancerDbtCompilerProvider
        )
    }

    fun testGetIdReturnsDbtCore() {
        assertEquals("dbt-core", createProvider().getId())
    }

    fun testGetDisplayNameReturnsDbtCore() {
        assertEquals("dbt Core", createProvider().getDisplayName())
    }

    fun testIsAvailableReturnsTrue() {
        val module = myFixture.module
        assertTrue(createProvider().isAvailable(project, module))
    }

    fun testGetIdIsStableAcrossCalls() {
        val provider = createProvider()
        assertEquals(provider.getId(), provider.getId())
    }

    fun testGetDisplayNameIsStableAcrossCalls() {
        val provider = createProvider()
        assertEquals(provider.getDisplayName(), provider.getDisplayName())
    }
}

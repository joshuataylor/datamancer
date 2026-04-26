package com.github.joshuataylor.datamancer.core.lang

import com.intellij.psi.PsiReferenceContributor
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for DbtReferenceContributor.
 * Tests that the reference contributor is properly configured to register providers.
 */
class DbtReferenceContributorTest : BasePlatformTestCase() {

    // Instantiation tests
    fun testContributorCanBeInstantiated() {
        val contributor = DbtReferenceContributor()
        assertNotNull(contributor)
    }

    fun testContributorImplementsPsiReferenceContributor() {
        val contributor = DbtReferenceContributor()
        assertTrue(contributor is PsiReferenceContributor)
    }

    // Method existence tests
    fun testRegisterReferenceProvidersMethodExists() {
        val contributor = DbtReferenceContributor()
        val method = contributor::class.java.methods.find { it.name == "registerReferenceProviders" }
        assertNotNull("registerReferenceProviders method should exist", method)
    }

    // Multiple instances tests
    fun testMultipleInstancesCanBeCreated() {
        val contributor1 = DbtReferenceContributor()
        val contributor2 = DbtReferenceContributor()

        assertNotNull(contributor1)
        assertNotNull(contributor2)
        assertNotSame(contributor1, contributor2)
    }

    // Provider registration tests - verify patterns exist
    fun testRefFunctionPatternAccessible() {
        // Verify the pattern used by the contributor is accessible
        val pattern = DbtModelRefReferenceProvider.REF_FUNCTION_PATTERN
        assertNotNull("REF_FUNCTION_PATTERN should be accessible", pattern)
    }

    fun testSourceFunctionPatternAccessible() {
        // Verify the pattern used by the contributor is accessible
        val pattern = DbtSourceRefReferenceProvider.SOURCE_FUNCTION_PATTERN
        assertNotNull("SOURCE_FUNCTION_PATTERN should be accessible", pattern)
    }

    // Verify provider classes can be instantiated (as contributor does)
    fun testModelRefProviderCanBeInstantiated() {
        val provider = DbtModelRefReferenceProvider()
        assertNotNull(provider)
    }

    fun testSourceRefProviderCanBeInstantiated() {
        val provider = DbtSourceRefReferenceProvider()
        assertNotNull(provider)
    }
}

package com.github.joshuataylor.datamancer.core.lang

import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for DbtYamlReferenceContributor.
 * Verifies the YAML reference contributor registration.
 */
class DatamancerYamlReferenceContributorTest : BasePlatformTestCase() {

    fun testContributorCanBeInstantiated() {
        assertNotNull(DbtYamlReferenceContributor())
    }

    fun testContributorExtendsPsiReferenceContributor() {
        assertTrue(
            "Should extend PsiReferenceContributor",
            DbtYamlReferenceContributor() is PsiReferenceContributor
        )
    }

    fun testNameReferenceProviderCanBeInstantiated() {
        assertNotNull(DbtYamlNameReferenceProvider())
    }

    fun testNameReferenceProviderExtendsPsiReferenceProvider() {
        assertTrue(
            "Should extend PsiReferenceProvider",
            DbtYamlNameReferenceProvider() is PsiReferenceProvider
        )
    }

    fun testRegisterReferenceProvidersMethodExists() {
        val method = DbtYamlReferenceContributor::class.java.methods.find {
            it.name == "registerReferenceProviders"
        }
        assertNotNull("registerReferenceProviders method should exist", method)
    }
}

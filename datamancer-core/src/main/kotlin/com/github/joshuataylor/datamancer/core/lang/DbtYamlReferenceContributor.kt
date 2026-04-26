package com.github.joshuataylor.datamancer.core.lang

import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar
import org.jetbrains.yaml.psi.YAMLScalar

/**
 * Registers reference providers for dbt constructs in YAML schema files.
 *
 * Enables go-to-definition from YAML names to SQL files:
 * - Model names under `models:` navigate to the corresponding `.sql` file
 * - Source/table names under `sources:` navigate to SQL files that use that source
 * - Generic test names under `data_tests:` navigate to the test definition `.sql` file
 */
class DbtYamlReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(YAMLScalar::class.java),
            DbtYamlNameReferenceProvider()
        )

        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(YAMLScalar::class.java),
            DbtYamlDataTestReferenceProvider()
        )
    }
}

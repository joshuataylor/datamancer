package com.github.joshuataylor.datamancer.core.lang

import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar

/**
 * Registers reference providers for dbt-specific constructs in Jinja2 templates.
 *
 * This contributor enables IDE features like:
 * - Go-to-definition for model references in ref(), source(), var(), and macro calls
 * - Code completion for model, source, table, variable, and macro names
 * - Find usages for dbt models, sources, variables, and macros
 *
 * Registered for both Jinja2 and DjangoTemplate languages to cover all template contexts.
 */
class DbtReferenceContributor : PsiReferenceContributor() {

    /**
     * Registers reference providers for dbt constructs.
     *
     * Uses proper PSI patterns to match Jinja2FunctionCall elements,
     * rather than matching all elements and filtering with regex.
     *
     * Currently registers:
     * - DbtModelRefReferenceProvider for ref() function calls
     * - DbtSourceRefReferenceProvider for source() function calls
     * - DbtVarRefReferenceProvider for var() function calls
     * - DbtMacroRefReferenceProvider for custom macro calls
     */
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        // Register provider for ref() function calls
        registrar.registerReferenceProvider(
            DbtModelRefReferenceProvider.REF_FUNCTION_PATTERN,
            DbtModelRefReferenceProvider()
        )

        // Register provider for source() function calls
        registrar.registerReferenceProvider(
            DbtSourceRefReferenceProvider.SOURCE_FUNCTION_PATTERN,
            DbtSourceRefReferenceProvider()
        )

        // Register provider for var() function calls
        registrar.registerReferenceProvider(
            DbtVarRefReferenceProvider.VAR_FUNCTION_PATTERN,
            DbtVarRefReferenceProvider()
        )

        // Register provider for custom macro calls
        registrar.registerReferenceProvider(
            DbtMacroRefReferenceProvider.MACRO_FUNCTION_PATTERN,
            DbtMacroRefReferenceProvider()
        )
    }
}

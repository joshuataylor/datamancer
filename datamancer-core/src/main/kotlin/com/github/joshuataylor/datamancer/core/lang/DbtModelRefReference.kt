package com.github.joshuataylor.datamancer.core.lang

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.jinja.psi.Jinja2StringLiteral
import com.intellij.jinja.tags.Jinja2FunctionCall
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.ResolveResult
import com.intellij.sql.SqlFileType

/**
 * Reference implementation for dbt `ref()` function calls.
 *
 * This reference resolves model names in `{{ ref('model_name') }}` calls to:
 * - SQL files in models/ directories
 * - CSV files in seeds/ directories
 *
 * Provides code completion for available models and seeds.
 *
 * @param element The Jinja2FunctionCall element containing the ref() call
 * @param textRange The text range of the model name within the element
 * @param modelName The model name being referenced
 * @param stringLiteral The Jinja2StringLiteral containing the model name
 */
class DbtModelRefReference(
    element: Jinja2FunctionCall,
    textRange: TextRange,
    modelName: String,
    private val stringLiteral: Jinja2StringLiteral
) : DbtModelReferenceBase<Jinja2FunctionCall>(element, textRange, modelName) {

    /**
     * Resolves the reference to the target model or seed file.
     *
     * Resolution order:
     * 1. Try to find a SQL model file with the given name
     * 2. If not found, try to find a CSV seed file with the given name
     *
     * @param incompleteCode Whether to consider incomplete code during resolution
     * @return Array containing the resolved element, or an empty array if not found
     */
    override fun resolveInner(incompleteCode: Boolean): Array<ResolveResult> {
        val project = element.project

        // Try to find a SQL model file first
        val modelFile = DbtDirectories.findModel(project, modelName)
        if (modelFile != null) {
            return arrayOf(PsiElementResolveResult(modelFile))
        }

        // Try to find a CSV seed file
        val seedFile = DbtDirectories.findSeedFile(project, modelName)
        if (seedFile != null) {
            val psiManager = com.intellij.psi.PsiManager.getInstance(project)
            val psiFile = psiManager.findFile(seedFile)
            if (psiFile != null) {
                return arrayOf(PsiElementResolveResult(psiFile))
            }
        }

        // Not found
        return ResolveResult.EMPTY_ARRAY
    }

    /**
     * Provides code completion variants for ref() calls.
     *
     * Returns all available models and seeds in the project with appropriate icons
     * and type text to distinguish between SQL models and CSV seeds.
     *
     * Filters out the current file to avoid self-references.
     *
     * @return Array of lookup elements for code completion
     */
    override fun getVariants(): Array<Any> {
        val project = element.project
        val variants = mutableListOf<LookupElement>()
        val currentFile = element.containingFile?.originalFile?.virtualFile

        // Add all SQL models (except current file)
        val models = DbtDirectories.findAllModels(project)
        for (model in models) {
            if (model.virtualFile == currentFile) continue

            val name = DbtDirectories.getModelNameFromFile(model)
            val lookupElement = LookupElementBuilder
                .create(name)
                .withIcon(SqlFileType.INSTANCE.icon)
                .withTypeText("model")
                .withPresentableText(name)

            variants.add(lookupElement)
        }

        // Add all CSV seeds
        val seeds = DbtDirectories.findAllSeeds(project)
        for (seed in seeds) {
            val name = DbtDirectories.getSeedNameFromFile(seed)
            val lookupElement = LookupElementBuilder
                .create(name)
                .withIcon(com.intellij.icons.AllIcons.FileTypes.Text)
                .withTypeText("seed")
                .withPresentableText(name)

            variants.add(lookupElement)
        }

        return variants.toTypedArray()
    }
}

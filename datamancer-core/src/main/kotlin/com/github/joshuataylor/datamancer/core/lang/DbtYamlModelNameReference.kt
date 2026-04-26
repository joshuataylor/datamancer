package com.github.joshuataylor.datamancer.core.lang

import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.ResolveResult
import org.jetbrains.yaml.psi.YAMLScalar

/**
 * Reference from a dbt model name in a YAML schema file to its SQL model file.
 *
 * Resolves `name: customers` under `models:` to `models/customers.sql`.
 *
 * @param element The YAMLScalar containing the model name
 * @param textRange Text range of the model name within the element
 * @param modelName The model name to resolve
 */
class DbtYamlModelNameReference(
    element: YAMLScalar,
    textRange: TextRange,
    modelName: String
) : DbtModelReferenceBase<YAMLScalar>(element, textRange, modelName) {

    override fun handleElementRename(newElementName: String): PsiElement {
        return super.handleElementRename(FileUtilRt.getNameWithoutExtension(newElementName))
    }

    override fun resolveInner(incompleteCode: Boolean): Array<ResolveResult> {
        val project = element.project

        val modelFile = DbtDirectories.findModel(project, modelName)
        if (modelFile != null) {
            return arrayOf(PsiElementResolveResult(modelFile))
        }

        // Also check seeds (a YAML models: entry could reference a seed)
        val seedFile = DbtDirectories.findSeedFile(project, modelName)
        if (seedFile != null) {
            val psiFile = com.intellij.psi.PsiManager.getInstance(project).findFile(seedFile)
            if (psiFile != null) {
                return arrayOf(PsiElementResolveResult(psiFile))
            }
        }

        return ResolveResult.EMPTY_ARRAY
    }
}

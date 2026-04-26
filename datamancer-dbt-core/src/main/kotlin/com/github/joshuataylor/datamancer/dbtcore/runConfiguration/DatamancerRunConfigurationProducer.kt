package com.github.joshuataylor.datamancer.dbtcore.runConfiguration

import com.github.joshuataylor.datamancer.core.services.DatamancerDbtProjectIndexService
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

/**
 * Run configuration producer that creates dbt compile & run configurations
 * from the context of a dbt model file.
 *
 * This enables:
 * - Run gutter icons in SQL files within dbt projects
 * - Right-click "Run" context menu on dbt model files
 * - Automatic configuration based on file path
 */
class DatamancerRunConfigurationProducer : LazyRunConfigurationProducer<DatamancerRunConfiguration>() {

    override fun getConfigurationFactory(): ConfigurationFactory =
        DatamancerRunConfigurationType().configurationFactories.first()

    override fun setupConfigurationFromContext(
        configuration: DatamancerRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        val psiFile = context.psiLocation?.containingFile ?: return false
        val virtualFile = psiFile.virtualFile ?: return false

        // Check if this is a SQL file in a dbt project
        val dbtModelPath = getDbtModelPath(psiFile, virtualFile) ?: return false

        // Get the module for this file
        val module = ModuleUtil.findModuleForFile(virtualFile, context.project) ?: return false

        // Check if this module has a dbt project configuration
        val indexService = DatamancerDbtProjectIndexService.getInstance(context.project)
        val dbtConfig = indexService.getDbtConfigForModuleName(module.name) ?: return false

        // Configure the run configuration
        configuration.name = "dbt: ${virtualFile.nameWithoutExtension}"
        configuration.modelName = virtualFile.nameWithoutExtension
        configuration.moduleName = module.name

        // Use project defaults for data source and query limit
        configuration.dataSourceName = dbtConfig.defaultDataSource
        configuration.queryLimit = null // Use project default

        sourceElement.set(psiFile)
        return true
    }

    override fun isConfigurationFromContext(
        configuration: DatamancerRunConfiguration,
        context: ConfigurationContext
    ): Boolean {
        val psiFile = context.psiLocation?.containingFile ?: return false
        val virtualFile = psiFile.virtualFile ?: return false

        val dbtModelPath = getDbtModelPath(psiFile, virtualFile) ?: return false
        val module = ModuleUtil.findModuleForFile(virtualFile, context.project) ?: return false

        // Check if the configuration matches this file
        return configuration.modelName == virtualFile.nameWithoutExtension &&
               configuration.moduleName == module.name
    }

    /**
     * Get the dbt model path for a file.
     * Returns the relative path from the models directory, or the file name if in models root.
     *
     * For example:
     * - models/my_model.sql -> my_model
     * - models/staging/my_model.sql -> staging/my_model
     * - models/marts/core/my_model.sql -> marts/core/my_model
     */
    private fun getDbtModelPath(psiFile: PsiFile, virtualFile: VirtualFile): String? {
        // Only handle SQL files
        if (!virtualFile.name.endsWith(".sql")) {
            return null
        }

        val path = virtualFile.path

        // Check if this file is in a models directory
        val modelsIndex = path.lastIndexOf("/models/")
        if (modelsIndex == -1) {
            return null
        }

        // Extract the path after /models/
        val relativePath = path.substring(modelsIndex + "/models/".length)

        // Remove the .sql extension
        return relativePath.removeSuffix(".sql")
    }
}

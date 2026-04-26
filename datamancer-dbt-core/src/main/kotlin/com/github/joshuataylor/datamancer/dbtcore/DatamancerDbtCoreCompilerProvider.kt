package com.github.joshuataylor.datamancer.dbtcore

import com.github.joshuataylor.datamancer.core.CompiledDbtModel
import com.github.joshuataylor.datamancer.core.api.DatamancerDbtCompilerProvider
import com.github.joshuataylor.datamancer.core.workspace.DatamancerProjectConfig
import com.github.joshuataylor.datamancer.dbtcore.compile.DatamancerCompileService
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project

/**
 * dbt Core compiler backend.
 * Uses the Python dbt CLI to compile dbt models.
 */
class DatamancerDbtCoreCompilerProvider : DatamancerDbtCompilerProvider {

    override fun getId(): String = "dbt-core"

    override fun getDisplayName(): String = "dbt Core"

    override fun isAvailable(project: Project, module: Module): Boolean {
        // dbt-core is available if the compile service can find a Python SDK
        return true
    }

    override suspend fun compile(
        project: Project,
        module: Module,
        config: DatamancerProjectConfig,
        modelName: String,
    ): Result<CompiledDbtModel> {
        val compileService = DatamancerCompileService.getInstance(project)
        return compileService.compileModel(
            module = module,
            modelName = modelName,
            projectRoot = config.projectRoot,
            environmentVariables = config.environmentVariables,
        )
    }
}

package com.github.joshuataylor.datamancer.dbtfusion

import com.github.joshuataylor.datamancer.core.CompiledDbtModel
import com.github.joshuataylor.datamancer.core.api.DatamancerDbtCompilerProvider
import com.github.joshuataylor.datamancer.core.workspace.DatamancerProjectConfig
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project

/**
 * dbt Fusion compiler backend (stub).
 * Will use the dbt Fusion parsing engine via LSP.
 */
class DatamancerDbtFusionCompilerProvider : DatamancerDbtCompilerProvider {

    override fun getId(): String = "dbt-fusion"

    override fun getDisplayName(): String = "dbt Fusion"

    override fun isAvailable(project: Project, module: Module): Boolean {
        // TODO: Check if dbt-fusion LSP server is available
        return false
    }

    override suspend fun compile(
        project: Project,
        module: Module,
        config: DatamancerProjectConfig,
        modelName: String,
    ): Result<CompiledDbtModel> {
        // TODO: Implement via LSP
        return Result.failure(UnsupportedOperationException("dbt Fusion backend not yet implemented"))
    }
}

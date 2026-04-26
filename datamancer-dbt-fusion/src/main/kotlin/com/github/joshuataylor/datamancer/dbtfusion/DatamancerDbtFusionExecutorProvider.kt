package com.github.joshuataylor.datamancer.dbtfusion

import com.github.joshuataylor.datamancer.core.api.DatamancerDbtExecutorProvider
import com.github.joshuataylor.datamancer.core.workspace.DatamancerProjectConfig
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * dbt Fusion query execution backend (stub).
 * Will use the dbt Fusion engine for execution.
 */
class DatamancerDbtFusionExecutorProvider : DatamancerDbtExecutorProvider {

    override fun getId(): String = "dbt-fusion"

    override fun getDisplayName(): String = "dbt Fusion"

    override fun execute(
        project: Project,
        config: DatamancerProjectConfig,
        virtualFile: VirtualFile,
        editor: Editor,
    ) {
        // TODO: Implement via LSP
        throw UnsupportedOperationException("dbt Fusion backend not yet implemented")
    }
}

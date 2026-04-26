package com.github.joshuataylor.datamancer.dbtcore

import com.github.joshuataylor.datamancer.core.api.DatamancerDbtExecutorProvider
import com.github.joshuataylor.datamancer.core.workspace.DatamancerProjectConfig
import com.github.joshuataylor.datamancer.dbtcore.execution.DatamancerQueryExecutor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * dbt Core query execution backend.
 * Executes compiled SQL via IntelliJ's database console.
 */
class DatamancerDbtCoreExecutorProvider : DatamancerDbtExecutorProvider {

    override fun getId(): String = "dbt-core"

    override fun getDisplayName(): String = "dbt Core"

    override fun execute(
        project: Project,
        config: DatamancerProjectConfig,
        virtualFile: VirtualFile,
        editor: Editor,
    ) {
        val executor = DatamancerQueryExecutor(project)
        executor.executeQuery(virtualFile, editor)
    }
}

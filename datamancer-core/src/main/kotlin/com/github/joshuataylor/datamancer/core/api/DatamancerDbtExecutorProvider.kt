package com.github.joshuataylor.datamancer.core.api

import com.github.joshuataylor.datamancer.core.workspace.DatamancerProjectConfig
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.annotations.ApiStatus

/**
 * Extension point for providing dbt query execution backends.
 *
 * Implementations are registered via the
 * [com.github.joshuataylor.datamancer.core.dbtExecutor] extension point.
 * Each backend provides its own way to execute compiled SQL.
 *
 * Implementations must be thread-safe.
 */
@ApiStatus.OverrideOnly
interface DatamancerDbtExecutorProvider {
    companion object {
        val EP_NAME: ExtensionPointName<DatamancerDbtExecutorProvider> =
            ExtensionPointName.create("com.github.joshuataylor.datamancer.core.dbtExecutor")
    }

    /** Must match the corresponding compiler's getId(). */
    fun getId(): String

    /** Display name shown in settings UI. */
    fun getDisplayName(): String

    /**
     * Execute compiled SQL for the given file.
     *
     * @param project the current project
     * @param config the dbt project configuration
     * @param virtualFile the file being executed
     * @param editor the editor containing the compiled SQL
     */
    fun execute(
        project: Project,
        config: DatamancerProjectConfig,
        virtualFile: VirtualFile,
        editor: Editor,
    )
}

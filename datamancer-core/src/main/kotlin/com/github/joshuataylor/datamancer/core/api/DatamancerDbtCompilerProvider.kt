package com.github.joshuataylor.datamancer.core.api

import com.github.joshuataylor.datamancer.core.CompiledDbtModel
import com.github.joshuataylor.datamancer.core.workspace.DatamancerProjectConfig
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus

/**
 * Extension point for providing dbt compilation backends.
 *
 * Implementations are registered via the
 * [com.github.joshuataylor.datamancer.core.dbtCompiler] extension point.
 * Each backend (e.g. dbt-core via Python CLI, dbt-fusion via LSP) provides
 * its own implementation.
 *
 * Implementations must be thread-safe.
 */
@ApiStatus.OverrideOnly
interface DatamancerDbtCompilerProvider {
    companion object {
        val EP_NAME: ExtensionPointName<DatamancerDbtCompilerProvider> =
            ExtensionPointName.create("com.github.joshuataylor.datamancer.core.dbtCompiler")
    }

    /** Unique identifier for this backend (e.g. "dbt-core", "dbt-fusion"). */
    fun getId(): String

    /** Display name shown in settings UI (e.g. "dbt Core", "dbt Fusion"). */
    fun getDisplayName(): String

    /** Whether this compiler backend is available for the given module. */
    fun isAvailable(project: Project, module: Module): Boolean

    /**
     * Compile a dbt model, returning the compiled SQL result.
     *
     * @param project the current project
     * @param module the module containing the dbt model
     * @param config the dbt project configuration
     * @param modelName the name of the model to compile
     * @return the compilation result, or a failure
     */
    suspend fun compile(
        project: Project,
        module: Module,
        config: DatamancerProjectConfig,
        modelName: String,
    ): Result<CompiledDbtModel>
}

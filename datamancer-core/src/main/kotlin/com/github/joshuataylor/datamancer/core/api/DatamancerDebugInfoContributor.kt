package com.github.joshuataylor.datamancer.core.api

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus

/**
 * Extension point for contributing debug information sections to the
 * Datamancer debug output file.
 *
 * Backend plugins (dbt-core, dbt-fusion, etc.) implement this to add
 * their own diagnostic sections to `datamancer-debug.txt`.
 *
 * Implementations must be thread-safe and must not perform long-running
 * operations since the debug action runs on a background thread.
 */
@ApiStatus.OverrideOnly
interface DatamancerDebugInfoContributor {
    companion object {
        val EP_NAME: ExtensionPointName<DatamancerDebugInfoContributor> =
            ExtensionPointName.create("com.github.joshuataylor.datamancer.core.debugInfoContributor")
    }

    /** Section title displayed in the debug output (e.g. "MANIFEST DATA"). */
    fun getSectionTitle(): String

    /** Appends debug information to the builder. Called synchronously from the debug action. */
    fun appendDebugInfo(project: Project, builder: StringBuilder)
}

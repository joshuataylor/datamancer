package com.github.joshuataylor.datamancer.dbtcore.mise

import com.github.l34130.mise.core.MiseHelper
import com.github.l34130.mise.core.setting.MiseProjectSettings
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

/**
 * Bridge to the intellij-mise plugin. Only classloaded when mise is installed.
 * Call via [DatamancerMiseIntegration] which guards against [NoClassDefFoundError].
 */
object DatamancerMiseHelper {
    /**
     * Whether mise is enabled at the project level (master toggle).
     */
    fun isMiseEnabledForProject(project: Project): Boolean {
        return project.service<MiseProjectSettings>().state.useMiseDirEnv
    }

    /**
     * Get mise environment variables for the given working directory.
     * Returns empty map if mise is disabled or an error occurs.
     */
    fun getMiseEnvironmentVariables(
        project: Project,
        workingDirectory: String,
    ): Map<String, String> {
        return MiseHelper.getMiseEnvVarsOrNotify(project, workingDirectory)
    }
}

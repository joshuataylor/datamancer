package com.github.joshuataylor.datamancer.dbtcore.manifest

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.waitForSmartMode
import com.intellij.openapi.startup.ProjectActivity
import kotlin.coroutines.cancellation.CancellationException

/**
 * Startup activity that triggers the initial manifest.json scan for dbt-core projects.
 *
 * Waits for smart mode so the index service has had time to discover dbt projects before
 * we attempt to load their manifests.
 */
class DatamancerManifestStartupActivity : ProjectActivity {
    private val log = logger<DatamancerManifestStartupActivity>()

    override suspend fun execute(project: Project) {
        log.debug("Starting manifest startup activity for project: ${project.name}")
        try {
            project.waitForSmartMode()
            DatamancerManifestService.getInstance(project).start()
            log.debug("Manifest startup activity completed for project: ${project.name}")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.error("Error during manifest startup activity for project: ${project.name}", e)
        }
    }
}

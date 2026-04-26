package com.github.joshuataylor.datamancer.core.services

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.waitForSmartMode
import com.intellij.openapi.startup.ProjectActivity
import kotlin.coroutines.cancellation.CancellationException

/**
 * Startup activity that triggers dbt project discovery when the project opens.
 */
class DatamancerProjectStartupActivity : ProjectActivity {
    private val log = logger<DatamancerProjectStartupActivity>()

    override suspend fun execute(project: Project) {
        log.debug("Starting dbt project startup activity for project: ${project.name}")

        try {
            // Wait for file indexing to complete so FilenameIndex can find dbt_project.yml files
            log.debug("Waiting for smart mode before discovering dbt projects")
            project.waitForSmartMode()

            // Trigger discovery - this will find all dbt_project.yml files and associate them with modules
            log.debug("Initialising dbt project discovery service")
            val discoveryService = DatamancerProjectDiscoveryService.getInstance(project)

            log.debug("Running dbt project discovery")
            discoveryService.discoverAndAssociate()

            // Start the index service to begin monitoring workspace changes
            log.debug("Initialising dbt project index service")
            val indexService = DatamancerDbtProjectIndexService.getInstance(project)

            log.debug("Starting dbt project index service")
            indexService.start()

            log.debug("dbt project startup activity completed successfully for project: ${project.name}")
        } catch (e: CancellationException) {
            // CancellationException is a control-flow exception and must be rethrown without logging
            throw e
        } catch (e: Exception) {
            log.error("Error during dbt project startup activity for project: ${project.name}", e)
            throw e
        }
    }
}

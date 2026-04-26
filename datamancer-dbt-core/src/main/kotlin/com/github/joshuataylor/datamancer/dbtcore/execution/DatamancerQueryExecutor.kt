package com.github.joshuataylor.datamancer.dbtcore.execution

import com.github.joshuataylor.datamancer.core.services.DatamancerDbtProjectIndexService
import com.intellij.database.console.JdbcConsole
import com.intellij.database.console.JdbcConsoleProvider
import com.intellij.database.console.session.DatabaseSession
import com.intellij.database.console.session.DatabaseSessionManager
import com.intellij.database.dataSource.LocalDataSource
import com.intellij.database.dataSource.LocalDataSourceManager
import com.intellij.database.run.ConsoleDataRequest
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * Handles the execution of queries in an IntelliJ platform plugin.
 * Automatically determines the appropriate data source based on the file's module
 * and its dbt project configuration from the workspace model.
 *
 * @property project the current project in which the query is executed
 */
class DatamancerQueryExecutor(
    private val project: Project,
) {
    private val log = logger<DatamancerQueryExecutor>()

    companion object {
        private val log = logger<DatamancerQueryExecutor>()
        /**
         * Retrieves a data source by name for the given project.
         *
         * @param project the project from which to retrieve the data source
         * @param dataSourceName The name of the datasource to use.
         * @return the matching data source, or null if not found
         */
        fun getDataSource(
            project: Project,
            dataSourceName: String,
        ): LocalDataSource? {
            log.debug("Looking for data source: $dataSourceName in project: ${project.name}")
            val dataSource = LocalDataSourceManager.getInstance(project).dataSources.find { it.name == dataSourceName }
            if (dataSource != null) {
                log.debug("Found data source: $dataSourceName")
            } else {
                log.warn("Data source not found: $dataSourceName")
            }
            return dataSource
        }
    }

    /**
     * Executes a query based on a given virtual file and editor.
     * Automatically determines the data source from the file's module configuration.
     *
     * @param virtualFile the virtual file associated with the query
     * @param editor the editor from which the query execution is triggered
     * @throws IllegalStateException if module or data source cannot be determined
     */
    fun executeQuery(
        virtualFile: VirtualFile,
        editor: Editor,
    ) {
        log.debug("Executing query for file: ${virtualFile.path}")

        // Find the module containing this file
        log.debug("Finding module for file: ${virtualFile.path}")
        val module = ModuleUtil.findModuleForFile(virtualFile, project)
            ?: throw IllegalStateException("Cannot determine module for file: ${virtualFile.path}").also {
                log.error("Failed to find module for file: ${virtualFile.path}")
            }
        log.debug("Found module: ${module.name}")

        // Get the dbt configuration for this module
        // We use the index service's synchronous access to the in-memory cache
        // This avoids blocking operations since the cache is already populated
        log.debug("Getting dbt configuration for module: ${module.name}")
        val indexService = DatamancerDbtProjectIndexService.getInstance(project)
        val dbtConfig = indexService.getDbtConfigForModuleName(module.name)
            ?: throw IllegalStateException("No dbt project configuration found for module: ${module.name}").also {
                log.error("No dbt project configuration found for module: ${module.name}")
            }
        log.debug("Found dbt configuration with project root: ${dbtConfig.projectRoot}")

        // Get the data source from the configuration
        val dataSourceName = dbtConfig.defaultDataSource
            ?: throw IllegalStateException("No default data source configured for dbt project in module: ${module.name}").also {
                log.error("No default data source configured for dbt project in module: ${module.name}")
            }
        log.debug("Using data source: $dataSourceName")

        val dataSource = getDataSource(project, dataSourceName)
            ?: throw IllegalStateException("Data source '$dataSourceName' not found for module: ${module.name}").also {
                log.error("Data source '$dataSourceName' not found for module: ${module.name}")
            }

        // Execute the query with the determined data source
        val sessionName = virtualFile.name
        log.debug("Getting or opening database session: $sessionName")
        val databaseSession = getOrOpenDatabaseSession(project, dataSource, sessionName)
        log.debug("Database session obtained: ${databaseSession.title}")

        val existingJdbcConsole =
            databaseSession.clientsWithFile.firstOrNull { it.virtualFile.name == virtualFile.name } as? JdbcConsole

        // Assign or create a console based on the type of console2
        log.debug("Setting up console for query execution")
        val console =
            existingJdbcConsole ?: JdbcConsoleProvider.getValidConsole(project, virtualFile)
                ?: JdbcConsoleProvider.attachConsole(project, databaseSession, virtualFile)
                ?: throw Exception("Console could not be initialised").also {
                    log.error("Failed to initialise console for file: ${virtualFile.path}")
                }
        log.debug("Console ready: ${console.virtualFile.name}")

        log.debug("Creating console data request")
        val consoleDataRequest =
            ConsoleDataRequest.newConsoleRequest(console, editor, console.scriptModel, false)
                ?: throw Exception("ConsoleDataRequest could not be created").also {
                    log.error("Failed to create ConsoleDataRequest for file: ${virtualFile.path}")
                }

        log.debug("Processing request")
        console.messageBus.dataProducer.processRequest(consoleDataRequest)
        log.debug("Query execution completed for file: ${virtualFile.path}")
    }

    /**
     * Retrieves or opens a database session for the given project, data source, and session name.
     *
     * @param project the project for the database session
     * @param dataSource the data source for the database session
     * @param sessionName the name of the session to retrieve or open
     * @return an existing or a newly opened database session
     */
    private fun getOrOpenDatabaseSession(
        project: Project,
        dataSource: LocalDataSource,
        sessionName: String,
    ): DatabaseSession {
        log.debug("Looking for existing database session: $sessionName")
        val existingSession = DatabaseSessionManager.getSessions(project).find {
            it.title == sessionName && it.project == project
        }

        return if (existingSession != null) {
            log.debug("Found existing database session: $sessionName")
            existingSession
        } else {
            log.debug("Opening new database session: $sessionName for data source: ${dataSource.name}")
            val newSession = DatabaseSessionManager.openSession(project, dataSource, sessionName)
            log.debug("Opened new database session: $sessionName")
            newSession
        }
    }
}

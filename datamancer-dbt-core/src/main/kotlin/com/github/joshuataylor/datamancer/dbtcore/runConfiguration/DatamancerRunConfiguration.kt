package com.github.joshuataylor.datamancer.dbtcore.runConfiguration

import com.github.joshuataylor.datamancer.dbtcore.mise.DatamancerMiseIntegration
import com.github.joshuataylor.datamancer.dbtcore.execution.DatamancerQueryExecutor
import com.github.joshuataylor.datamancer.dbtcore.compile.DatamancerCompileService
import com.github.joshuataylor.datamancer.core.services.DatamancerDbtProjectIndexService
import com.intellij.database.console.JdbcConsole
import com.intellij.database.console.JdbcConsoleProvider
import com.intellij.database.console.session.DatabaseSessionManager
import com.intellij.database.run.ConsoleDataRequest
import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.process.NopProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightVirtualFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Run configuration for compiling and executing dbt models.
 */
class DatamancerRunConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    name: String,
) : RunConfigurationBase<DatamancerRunConfigurationOptions>(project, factory, name) {

    private val log = logger<DatamancerRunConfiguration>()

    override fun getOptions(): DatamancerRunConfigurationOptions =
        super.getOptions() as DatamancerRunConfigurationOptions

    var modelName: String?
        get() = options.modelName
        set(value) {
            options.modelName = value
        }

    var moduleName: String?
        get() = options.moduleName
        set(value) {
            options.moduleName = value
        }

    var dataSourceName: String?
        get() = options.dataSourceName
        set(value) {
            options.dataSourceName = value
        }

    var queryLimit: Int?
        get() = options.queryLimit
        set(value) {
            options.queryLimit = value
        }

    var environmentVariables: MutableMap<String, String>
        get() = options.environmentVariables
        set(value) {
            options.environmentVariables = value
        }

    var useMiseEnvironment: Boolean
        get() = options.useMiseEnvironment
        set(value) {
            options.useMiseEnvironment = value
        }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> =
        DatamancerSettingsEditor(project)

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        return DatamancerRunProfileState(this, environment)
    }

    /**
     * Run profile state that handles the compilation and execution.
     */
    private inner class DatamancerRunProfileState(
        private val configuration: DatamancerRunConfiguration,
        private val environment: ExecutionEnvironment,
    ) : RunProfileState {

        override fun execute(executor: Executor?, runner: ProgramRunner<*>): ExecutionResult {
            val consoleView = com.intellij.execution.impl.ConsoleViewImpl(project, true)
            val processHandler = NopProcessHandler()

            consoleView.attachToProcess(processHandler)
            consoleView.print("dbt Compile & Run: ${configuration.modelName}\n", ConsoleViewContentType.NORMAL_OUTPUT)
            consoleView.print("-".repeat(50) + "\n", ConsoleViewContentType.NORMAL_OUTPUT)

            // Launch the async compilation and execution
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    executeCompileAndRun(consoleView)
                } finally {
                    processHandler.destroyProcess()
                }
            }

            processHandler.startNotify()
            return DefaultExecutionResult(consoleView, processHandler)
        }

        private suspend fun executeCompileAndRun(consoleView: ConsoleView) {
            val modelName = configuration.modelName
            if (modelName.isNullOrBlank()) {
                printError(consoleView, "Model name is not specified")
                return
            }

            val moduleName = configuration.moduleName
            if (moduleName.isNullOrBlank()) {
                printError(consoleView, "Module is not specified")
                return
            }

            // Get the module
            val module = ModuleManager.getInstance(project).findModuleByName(moduleName)
            if (module == null) {
                printError(consoleView, "Module not found: $moduleName")
                return
            }

            // Get the dbt project config
            val indexService = DatamancerDbtProjectIndexService.getInstance(project)
            val dbtConfig = indexService.getDbtConfigForModuleName(moduleName)
            if (dbtConfig == null) {
                printError(consoleView, "No dbt project configuration found for module: $moduleName")
                return
            }

            printInfo(consoleView, "Compiling model: $modelName")
            printInfo(consoleView, "Project root: ${dbtConfig.projectRoot}")

            // Merge environment variables (run config overrides project defaults)
            val envVars = dbtConfig.environmentVariables.toMutableMap()
            envVars.putAll(configuration.environmentVariables)

            // Inject mise environment variables if enabled at both project and run-config level
            if (dbtConfig.useMiseEnvironment && configuration.useMiseEnvironment) {
                val miseEnvVars = DatamancerMiseIntegration.getMiseEnvironmentVariables(
                    project = project,
                    workingDirectory = dbtConfig.projectRoot,
                )
                if (miseEnvVars.isNotEmpty()) {
                    printInfo(consoleView, "Loaded ${miseEnvVars.size} environment variables from mise")
                    // Mise vars go first; explicit project/run-config vars take precedence
                    val merged = miseEnvVars.toMutableMap()
                    merged.putAll(envVars)
                    envVars.clear()
                    envVars.putAll(merged)
                }
            }

            // Compile the model
            val compileService = DatamancerCompileService.getInstance(project)
            val result = compileService.compileModel(
                module = module,
                modelName = modelName,
                projectRoot = dbtConfig.projectRoot,
                environmentVariables = envVars
            )

            if (result.isFailure) {
                val error = result.exceptionOrNull()?.message ?: "Unknown compilation error"
                printError(consoleView, "Compilation failed: $error")
                return
            }

            val compiledModel = result.getOrNull()!!
            val compiledCode = compiledModel.compiledCode
            if (compiledCode.isNullOrBlank()) {
                printError(consoleView, "Compilation returned empty SQL")
                return
            }

            printSuccess(consoleView, "Compilation successful")
            printInfo(consoleView, "Compiled SQL length: ${compiledCode.length} characters")

            // Apply query limit
            val effectiveLimit = configuration.queryLimit ?: dbtConfig.queryLimit
            val finalSql = if (effectiveLimit > 0) {
                printInfo(consoleView, "Applying query limit: $effectiveLimit rows")
                "SELECT * FROM (\n$compiledCode\n) datamancer_limited\nLIMIT $effectiveLimit"
            } else {
                compiledCode
            }

            // Get the data source
            val effectiveDataSource = configuration.dataSourceName?.takeIf { it.isNotBlank() }
                ?: dbtConfig.defaultDataSource
            if (effectiveDataSource.isNullOrBlank()) {
                printError(consoleView, "No data source configured")
                return
            }

            val dataSource = DatamancerQueryExecutor.getDataSource(project, effectiveDataSource)
            if (dataSource == null) {
                printError(consoleView, "Data source not found: $effectiveDataSource")
                return
            }

            printInfo(consoleView, "Executing SQL on data source: $effectiveDataSource")

            // Execute the SQL on the EDT
            ApplicationManager.getApplication().invokeLater {
                try {
                    executeSql(consoleView, finalSql, dataSource, modelName)
                } catch (e: Exception) {
                    log.error("Error executing SQL", e)
                    printError(consoleView, "Execution error: ${e.message}")
                }
            }
        }

        private fun executeSql(
            consoleView: ConsoleView,
            sql: String,
            dataSource: com.intellij.database.dataSource.LocalDataSource,
            modelName: String
        ) {
            // Create a light virtual file with the compiled SQL
            val virtualFile = LightVirtualFile("$modelName.sql", sql)

            // Get or create a database session
            val sessionName = "dbt: $modelName"
            val existingSession = DatabaseSessionManager.getSessions(project).find {
                it.title == sessionName && it.project == project
            }
            val session = existingSession ?: DatabaseSessionManager.openSession(project, dataSource, sessionName)

            // Get or create a JDBC console
            val existingConsole = session.clientsWithFile
                .filterIsInstance<JdbcConsole>()
                .firstOrNull { it.virtualFile.name == virtualFile.name }

            val jdbcConsole = existingConsole
                ?: JdbcConsoleProvider.getValidConsole(project, virtualFile)
                ?: JdbcConsoleProvider.attachConsole(project, session, virtualFile)

            if (jdbcConsole == null) {
                printError(consoleView, "Could not create database console")
                return
            }

            // Create an in-memory editor for the ConsoleDataRequest without opening a tab
            val editorFactory = EditorFactory.getInstance()
            val document = editorFactory.createDocument(sql)
            val editor = editorFactory.createEditor(document, project)

            try {
                // Create and execute the request
                val request = ConsoleDataRequest.newConsoleRequest(jdbcConsole, editor, jdbcConsole.scriptModel, false)
                if (request == null) {
                    printError(consoleView, "Could not create database request")
                    return
                }

                printSuccess(consoleView, "Executing query...")
                jdbcConsole.messageBus.dataProducer.processRequest(request)
            } finally {
                editorFactory.releaseEditor(editor)
            }
        }

        private fun printInfo(consoleView: ConsoleView, message: String) {
            ApplicationManager.getApplication().invokeLater {
                consoleView.print("[INFO] $message\n", ConsoleViewContentType.NORMAL_OUTPUT)
            }
        }

        private fun printSuccess(consoleView: ConsoleView, message: String) {
            ApplicationManager.getApplication().invokeLater {
                consoleView.print("[OK] $message\n", ConsoleViewContentType.SYSTEM_OUTPUT)
            }
        }

        private fun printError(consoleView: ConsoleView, message: String) {
            ApplicationManager.getApplication().invokeLater {
                consoleView.print("[ERROR] $message\n", ConsoleViewContentType.ERROR_OUTPUT)
            }
        }
    }
}

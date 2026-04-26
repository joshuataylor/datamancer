package com.github.joshuataylor.datamancer.dbtcore.compile

import com.github.joshuataylor.datamancer.core.CompiledDbtModel
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.python.sdk.PythonSdkType
import com.jetbrains.python.sdk.PythonSdkUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * Service that compiles dbt models by invoking the Python dbt CLI.
 * Extracts the bundled Python script and invokes it with the module's Python SDK.
 */
@Service(Service.Level.PROJECT)
class DatamancerCompileService(private val project: Project) {
    private val log = logger<DatamancerCompileService>()

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    // Lazy extraction of the Python script to a temp file
    private val pythonScript: File by lazy {
        extractPythonScript()
    }

    /**
     * Compile a dbt model and return the result.
     *
     * @param module The module containing the dbt project
     * @param modelName The name of the dbt model to compile
     * @param projectRoot The root directory of the dbt project
     * @param environmentVariables Additional environment variables for the dbt process
     * @return Result containing either the compiled model or an error
     */
    suspend fun compileModel(
        module: Module,
        modelName: String,
        projectRoot: String,
        environmentVariables: Map<String, String> = emptyMap()
    ): Result<CompiledDbtModel> = withContext(Dispatchers.IO) {
        log.debug("Compiling dbt model: $modelName in project: $projectRoot")

        // Get Python SDK for the module
        val pythonSdk = findPythonSdk(module)
        if (pythonSdk == null) {
            log.warn("No Python SDK found for module: ${module.name}")
            return@withContext Result.failure(
                IllegalStateException("No Python SDK configured for module: ${module.name}")
            )
        }

        val pythonPath = pythonSdk.homePath
        if (pythonPath == null) {
            log.warn("Python SDK has no home path: ${pythonSdk.name}")
            return@withContext Result.failure(
                IllegalStateException("Python SDK '${pythonSdk.name}' has no home path configured")
            )
        }
        log.debug("Using Python interpreter: $pythonPath")
        log.debug("Python script: ${pythonScript.absolutePath}")

        try {
            // Build the command line
            val commandLine = GeneralCommandLine()
                .withExePath(pythonPath)
                .withParameters(pythonScript.absolutePath, modelName)
                .withWorkDirectory(projectRoot)
                .withEnvironment(environmentVariables)
                .withCharset(StandardCharsets.UTF_8)

            log.debug("Executing command: ${commandLine.commandLineString}")
            log.debug("Working directory: $projectRoot")

            // Execute the process
            val processHandler = CapturingProcessHandler(commandLine)
            val output = processHandler.runProcess(60_000) // 60 second timeout

            log.debug("Process exit code: ${output.exitCode}")
            log.debug("Process stdout: ${output.stdout}")
            log.debug("Process stderr: ${output.stderr}")

            // Parse the JSON output from stdout
            val jsonOutput = output.stdout.trim()
            if (jsonOutput.isEmpty()) {
                val errorDetails = buildString {
                    appendLine("No output from dbt compilation")
                    appendLine("Exit code: ${output.exitCode}")
                    appendLine("Command: ${commandLine.commandLineString}")
                    appendLine("Working directory: $projectRoot")
                    if (output.stderr.isNotBlank()) {
                        appendLine("Stderr: ${output.stderr}")
                    }
                }
                return@withContext Result.failure(RuntimeException(errorDetails))
            }

            // Find the JSON line (last line starting with {)
            val jsonLine = jsonOutput.lines()
                .lastOrNull { it.trim().startsWith("{") }

            if (jsonLine == null) {
                val errorDetails = buildString {
                    appendLine("No JSON response in dbt output")
                    appendLine("Exit code: ${output.exitCode}")
                    appendLine("Stdout: $jsonOutput")
                    if (output.stderr.isNotBlank()) {
                        appendLine("Stderr: ${output.stderr}")
                    }
                }
                return@withContext Result.failure(RuntimeException(errorDetails))
            }

            val compiledModel = json.decodeFromString<CompiledDbtModel>(jsonLine)

            if (compiledModel.success) {
                log.debug("Successfully compiled model: $modelName")
                Result.success(compiledModel)
            } else {
                val error = compiledModel.error ?: "Unknown compilation error"
                log.warn("Compilation failed for model $modelName: $error")
                Result.failure(RuntimeException(error))
            }
        } catch (e: Exception) {
            log.error("Error compiling model $modelName", e)
            Result.failure(e)
        }
    }

    /**
     * Find the Python SDK for the given module.
     * Tries multiple approaches:
     * 1. Module-level Python SDK (via Python facet)
     * 2. Project-level SDK (if it's a Python SDK)
     * 3. Any Python SDK in the SDK table
     */
    private fun findPythonSdk(module: Module): Sdk? {
        // 1. Try module-level Python SDK
        try {
            val moduleSdk = PythonSdkUtil.findPythonSdk(module)
            if (moduleSdk != null) {
                log.debug("Found module-level Python SDK: ${moduleSdk.name}")
                return moduleSdk
            }
        } catch (e: Exception) {
            log.debug("Error finding module Python SDK: ${e.message}")
        }

        // 2. Try project-level SDK
        val projectSdk = ProjectRootManager.getInstance(project).projectSdk
        if (projectSdk != null && projectSdk.sdkType is PythonSdkType) {
            log.debug("Found project-level Python SDK: ${projectSdk.name}")
            return projectSdk
        }

        // 3. Try to find any Python SDK in the SDK table
        val allPythonSdks = ProjectJdkTable.getInstance().allJdks
            .filter { it.sdkType is PythonSdkType }
        if (allPythonSdks.isNotEmpty()) {
            val sdk = allPythonSdks.first()
            log.debug("Found Python SDK in SDK table: ${sdk.name}")
            return sdk
        }

        log.warn("No Python SDK found for module: ${module.name}")
        return null
    }

    /**
     * Extract the bundled Python script to a temporary file.
     */
    private fun extractPythonScript(): File {
        log.debug("Extracting Python compilation script")
        val resourcePath = "/dbt/dbtcompile.py"
        val inputStream = javaClass.getResourceAsStream(resourcePath)
            ?: throw IllegalStateException("Could not find bundled Python script: $resourcePath")

        val tempFile = FileUtil.createTempFile("datamancer_compile", ".py", true)
        inputStream.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        log.debug("Extracted Python script to: ${tempFile.absolutePath}")
        return tempFile
    }

    companion object {
        fun getInstance(project: Project): DatamancerCompileService =
            project.getService(DatamancerCompileService::class.java)
    }
}

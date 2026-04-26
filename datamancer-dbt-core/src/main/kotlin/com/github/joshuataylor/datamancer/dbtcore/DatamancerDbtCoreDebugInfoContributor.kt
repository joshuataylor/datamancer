package com.github.joshuataylor.datamancer.dbtcore

import com.github.joshuataylor.datamancer.core.api.DatamancerDebugInfoContributor
import com.github.joshuataylor.datamancer.core.services.DatamancerDbtProjectIndexService
import com.github.joshuataylor.datamancer.dbtcore.manifest.DatamancerManifestService
import com.github.joshuataylor.datamancer.dbtcore.mise.DatamancerMiseIntegration
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import com.jetbrains.python.sdk.PythonSdkType
import com.jetbrains.python.sdk.PythonSdkUtil
import java.nio.charset.StandardCharsets

/**
 * Contributes dbt-core backend environment and manifest data to the
 * Datamancer debug output.
 *
 * Reports:
 * - Python SDK configuration (path, version)
 * - dbt version (queried from the installed dbt package)
 * - Mise plugin status and project-level toggle
 * - Loaded manifest details per project root
 */
class DatamancerDbtCoreDebugInfoContributor : DatamancerDebugInfoContributor {

    override fun getSectionTitle(): String = "DBT-CORE BACKEND"

    override fun appendDebugInfo(project: Project, builder: StringBuilder) {
        appendSafe(builder, "Python SDK") { appendPythonSdkInfo(project, builder) }
        appendSafe(builder, "dbt Version") { appendDbtVersionInfo(project, builder) }
        appendSafe(builder, "Mise Integration") { appendMiseInfo(project, builder) }
        appendSafe(builder, "Manifest Data") { appendManifestInfo(project, builder) }
    }

    private inline fun appendSafe(builder: StringBuilder, section: String, block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            builder.appendLine("ERROR collecting $section: ${e.javaClass.simpleName}: ${e.message}")
            builder.appendLine()
        }
    }

    private fun appendPythonSdkInfo(project: Project, builder: StringBuilder) {
        builder.appendLine("Python SDK:")

        // Project-level SDK
        val projectSdk = ProjectRootManager.getInstance(project).projectSdk
        if (projectSdk != null && projectSdk.sdkType is PythonSdkType) {
            builder.appendLine("  Project SDK: ${projectSdk.name}")
            builder.appendLine("  Path: ${projectSdk.homePath ?: "(no home path)"}")
            builder.appendLine("  Version: ${projectSdk.versionString ?: "(unknown)"}")
        } else {
            builder.appendLine("  Project SDK: (not a Python SDK)")
        }

        // Module-level SDKs for dbt projects
        val indexService = DatamancerDbtProjectIndexService.getInstance(project)
        val configs = indexService.getAllDbtConfigsSync()
        if (configs.isNotEmpty()) {
            val moduleManager = com.intellij.openapi.module.ModuleManager.getInstance(project)
            for ((moduleName, _) in configs) {
                val module = moduleManager.findModuleByName(moduleName) ?: continue
                val moduleSdk = try {
                    PythonSdkUtil.findPythonSdk(module)
                } catch (_: Exception) {
                    null
                }
                if (moduleSdk != null) {
                    builder.appendLine("  Module '$moduleName' SDK: ${moduleSdk.name}")
                    builder.appendLine("    Path: ${moduleSdk.homePath ?: "(no home path)"}")
                    builder.appendLine("    Version: ${moduleSdk.versionString ?: "(unknown)"}")
                }
            }
        }

        // All registered Python SDKs
        val allPythonSdks = ProjectJdkTable.getInstance().allJdks
            .filter { it.sdkType is PythonSdkType }
        builder.appendLine("  Registered Python SDKs: ${allPythonSdks.size}")
        for (sdk in allPythonSdks) {
            builder.appendLine("    - ${sdk.name}: ${sdk.homePath ?: "(no path)"}")
        }
        builder.appendLine()
    }

    /**
     * Queries the installed dbt version by running a short Python snippet
     * against the project's configured Python interpreter.
     */
    private fun appendDbtVersionInfo(project: Project, builder: StringBuilder) {
        builder.appendLine("dbt Version:")

        val pythonSdk = findBestPythonSdk(project)
        val pythonPath = pythonSdk?.homePath

        if (pythonPath == null) {
            builder.appendLine("  (no Python SDK configured, cannot query dbt version)")
            builder.appendLine()
            return
        }

        val version = queryPythonPackageVersion(pythonPath, "dbt.version", "installed")
        builder.appendLine("  dbt-core: ${version ?: "(not installed or import failed)"}")

        val adapterVersion = queryPythonPackageVersion(
            pythonPath,
            "importlib.metadata",
            """[f"{d.metadata['Name']}=={d.version}" for d in distributions() if d.metadata['Name'].startswith('dbt-')]"""
        )
        if (adapterVersion != null) {
            builder.appendLine("  Installed dbt packages: $adapterVersion")
        }

        builder.appendLine()
    }

    /**
     * Runs `python -c "import <module>; print(<attr>)"` and returns the trimmed
     * stdout, or null on any failure.
     */
    private fun queryPythonPackageVersion(pythonPath: String, module: String, attr: String): String? {
        return try {
            val commandLine = GeneralCommandLine()
                .withExePath(pythonPath)
                .withParameters("-c", "from $module import *; print($attr)")
                .withCharset(StandardCharsets.UTF_8)

            val output = CapturingProcessHandler(commandLine)
                .runProcess(5_000)

            if (output.exitCode == 0) output.stdout.trim().ifEmpty { null } else null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Finds the best available Python SDK: project SDK first, then any
     * module-level SDK from a dbt project, then any registered Python SDK.
     */
    private fun findBestPythonSdk(project: Project): Sdk? {
        val projectSdk = ProjectRootManager.getInstance(project).projectSdk
        if (projectSdk != null && projectSdk.sdkType is PythonSdkType) {
            return projectSdk
        }

        val indexService = DatamancerDbtProjectIndexService.getInstance(project)
        val configs = indexService.getAllDbtConfigsSync()
        if (configs.isNotEmpty()) {
            val moduleManager = com.intellij.openapi.module.ModuleManager.getInstance(project)
            for ((moduleName, _) in configs) {
                val module = moduleManager.findModuleByName(moduleName) ?: continue
                val moduleSdk = try {
                    PythonSdkUtil.findPythonSdk(module)
                } catch (_: Exception) {
                    null
                }
                if (moduleSdk != null) return moduleSdk
            }
        }

        return ProjectJdkTable.getInstance().allJdks
            .firstOrNull { it.sdkType is PythonSdkType }
    }

    private fun appendMiseInfo(project: Project, builder: StringBuilder) {
        builder.appendLine("Mise Integration:")

        val misePluginId = PluginId.findId("com.github.l34130.mise")
        val misePlugin = misePluginId?.let { PluginManagerCore.getPlugin(it) }

        if (misePlugin == null) {
            builder.appendLine("  Plugin: not installed")
        } else {
            builder.appendLine("  Plugin: ${misePlugin.version} (${if (misePlugin.isEnabled) "enabled" else "disabled"})")
        }

        builder.appendLine("  Available: ${DatamancerMiseIntegration.isMisePluginAvailable()}")
        builder.appendLine("  Enabled for Project: ${DatamancerMiseIntegration.isMiseEnabledForProject(project)}")
        builder.appendLine()
    }

    private fun appendManifestInfo(project: Project, builder: StringBuilder) {
        builder.appendLine("Manifest Data:")

        val manifestService = DatamancerManifestService.getInstance(project)
        val manifests = manifestService.getAllManifests()

        if (manifests.isEmpty()) {
            builder.appendLine("  No manifests loaded.")
            builder.appendLine()
            return
        }

        builder.appendLine("  Loaded manifests: ${manifests.size}")
        builder.appendLine()

        for ((projectRoot, manifest) in manifests) {
            builder.appendLine("  Project Root: $projectRoot")
            builder.appendLine("    dbt Version: ${manifest.metadata.dbtVersion ?: "(unknown)"}")
            builder.appendLine("    Adapter Type: ${manifest.metadata.adapterType ?: "(unknown)"}")
            builder.appendLine("    Project Name: ${manifest.metadata.projectName ?: "(unknown)"}")
            builder.appendLine("    Generated At: ${manifest.metadata.generatedAt ?: "(unknown)"}")
            builder.appendLine("    Nodes: ${manifest.nodes.size}")
            builder.appendLine("    Sources: ${manifest.sources.size}")
            builder.appendLine("    Parent Map Entries: ${manifest.parentMap.size}")
            builder.appendLine("    Child Map Entries: ${manifest.childMap.size}")

            val nodesByType = manifest.nodes.values.groupBy { it.resourceType }
            if (nodesByType.isNotEmpty()) {
                builder.appendLine("    Nodes by Type:")
                for ((type, nodes) in nodesByType.entries.sortedBy { it.key }) {
                    builder.appendLine("      $type: ${nodes.size}")
                }
            }
            builder.appendLine()
        }
    }
}

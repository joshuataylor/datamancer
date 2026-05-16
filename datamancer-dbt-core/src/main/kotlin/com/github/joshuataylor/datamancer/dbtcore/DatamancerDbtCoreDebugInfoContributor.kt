package com.github.joshuataylor.datamancer.dbtcore

import com.github.joshuataylor.datamancer.core.api.DatamancerDebugInfoContributor
import com.github.joshuataylor.datamancer.core.services.DatamancerDbtProjectIndexService
import com.github.joshuataylor.datamancer.dbtcore.manifest.DatamancerManifestService
import com.github.joshuataylor.datamancer.dbtcore.mise.DatamancerMiseIntegration
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
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
 *
 * May be called from a background thread (Dispatchers.IO). Model and SDK
 * access is wrapped in read actions where needed.
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

    /**
     * Data collected under a read action for the Python SDK section.
     */
    private data class SdkInfoData(
        val projectSdkName: String?,
        val projectSdkPath: String?,
        val projectSdkVersion: String?,
        val isPythonSdk: Boolean,
        val moduleSdks: List<Triple<String, String, String?>>,
        val allPythonSdks: List<Pair<String, String?>>,
    )

    private fun appendPythonSdkInfo(project: Project, builder: StringBuilder) {
        builder.appendLine("Python SDK:")

        val sdkInfo = ReadAction.nonBlocking<SdkInfoData> {
            val projectSdk = ProjectRootManager.getInstance(project).projectSdk
            val isPython = projectSdk != null && projectSdk.sdkType is PythonSdkType

            val configs = DatamancerDbtProjectIndexService.getInstance(project).getAllDbtConfigsSync()
            val moduleManager = ModuleManager.getInstance(project)

            val moduleSdks = configs.mapNotNull { (moduleName, _) ->
                val module = moduleManager.findModuleByName(moduleName) ?: return@mapNotNull null
                val sdk = runCatching { PythonSdkUtil.findPythonSdk(module) }.getOrNull()
                    ?: return@mapNotNull null
                Triple(moduleName, sdk.name, sdk.homePath)
            }

            val allPythonSdks = ProjectJdkTable.getInstance().allJdks
                .filter { it.sdkType is PythonSdkType }
                .map { it.name to it.homePath }

            SdkInfoData(
                projectSdkName = projectSdk?.name,
                projectSdkPath = projectSdk?.homePath,
                projectSdkVersion = projectSdk?.versionString,
                isPythonSdk = isPython,
                moduleSdks = moduleSdks,
                allPythonSdks = allPythonSdks,
            )
        }.executeSynchronously()

        if (sdkInfo.isPythonSdk) {
            builder.appendLine("  Project SDK: ${sdkInfo.projectSdkName}")
            builder.appendLine("  Path: ${sdkInfo.projectSdkPath ?: "(no home path)"}")
            builder.appendLine("  Version: ${sdkInfo.projectSdkVersion ?: "(unknown)"}")
        } else {
            builder.appendLine("  Project SDK: (not a Python SDK)")
        }

        for ((moduleName, sdkName, sdkPath) in sdkInfo.moduleSdks) {
            builder.appendLine("  Module '$moduleName' SDK: $sdkName")
            builder.appendLine("    Path: ${sdkPath ?: "(no home path)"}")
        }

        builder.appendLine("  Registered Python SDKs: ${sdkInfo.allPythonSdks.size}")
        for ((name, path) in sdkInfo.allPythonSdks) {
            builder.appendLine("    - $name: ${path ?: "(no path)"}")
        }
        builder.appendLine()
    }

    /**
     * Queries the installed dbt version by running a short Python snippet
     * against the project's configured Python interpreter.
     */
    private fun appendDbtVersionInfo(project: Project, builder: StringBuilder) {
        builder.appendLine("dbt Version:")

        val pythonPath = findBestPythonPath(project)

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
     * Finds the home path of the best available Python SDK under a read action.
     */
    private fun findBestPythonPath(project: Project): String? {
        return ReadAction.nonBlocking<String?> {
            val projectSdk = ProjectRootManager.getInstance(project).projectSdk
            if (projectSdk != null && projectSdk.sdkType is PythonSdkType) {
                return@nonBlocking projectSdk.homePath
            }

            val configs = DatamancerDbtProjectIndexService.getInstance(project).getAllDbtConfigsSync()
            val moduleManager = ModuleManager.getInstance(project)
            for ((moduleName, _) in configs) {
                val module = moduleManager.findModuleByName(moduleName) ?: continue
                val sdk = runCatching { PythonSdkUtil.findPythonSdk(module) }.getOrNull()
                if (sdk?.homePath != null) return@nonBlocking sdk.homePath
            }

            ProjectJdkTable.getInstance().allJdks
                .firstOrNull { it.sdkType is PythonSdkType }
                ?.homePath
        }.executeSynchronously()
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

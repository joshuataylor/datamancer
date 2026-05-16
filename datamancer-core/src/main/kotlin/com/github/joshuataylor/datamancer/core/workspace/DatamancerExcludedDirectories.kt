package com.github.joshuataylor.datamancer.core.workspace

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.vfs.VfsUtilCore

/**
 * Utility for managing excluded directories in dbt projects.
 *
 * Provides methods to:
 * - Apply directory exclusions to IntelliJ module content entries via [applyExclusions]
 * - Check whether a file path falls inside an excluded directory via [isInExcludedDirectory]
 */
object DatamancerExcludedDirectories {
    private val log = logger<DatamancerExcludedDirectories>()

    /**
     * Marks the configured excluded directories as excluded folders on the module's content entry.
     *
     * Uses [ModuleRootModificationUtil.updateModel] which handles its own write action internally.
     * Idempotent: safe to call repeatedly with the same inputs.
     *
     * @param project The current project
     * @param moduleName The name of the module containing the dbt project
     * @param config The dbt project configuration with the excluded directory list
     */
    fun applyExclusions(project: Project, moduleName: String, config: DatamancerProjectConfig) {
        val module = ReadAction.nonBlocking<Module?> {
            ModuleManager.getInstance(project).findModuleByName(moduleName)
        }.executeSynchronously()
        if (module == null) {
            log.warn("Cannot apply exclusions: module '$moduleName' not found")
            return
        }

        val projectRoot = config.projectRoot.trimEnd('/')
        val excludeUrls = config.excludedDirectories
            .filter { it.isNotBlank() }
            .map { dirName -> VfsUtilCore.pathToUrl("$projectRoot/${dirName.trim()}") }
            .toSet()

        if (excludeUrls.isEmpty()) {
            log.debug("No directories to exclude for module '$moduleName'")
            return
        }

        ModuleRootModificationUtil.updateModel(module) { modifiableModel ->
            for (contentEntry in modifiableModel.contentEntries) {
                val contentUrl = contentEntry.url
                // Only modify content entries that cover the dbt project root
                val projectRootUrl = VfsUtilCore.pathToUrl(projectRoot)
                if (contentUrl != projectRootUrl && !projectRootUrl.startsWith("$contentUrl/")) {
                    continue
                }

                val existingExcludeUrls = contentEntry.excludeFolderUrls.toSet()

                // Add new exclusions that are not already present
                for (url in excludeUrls) {
                    if (url !in existingExcludeUrls) {
                        contentEntry.addExcludeFolder(url)
                        log.debug("Added exclude folder: $url (module: $moduleName)")
                    }
                }
            }
        }

        log.debug("Applied ${excludeUrls.size} directory exclusion(s) for module '$moduleName'")
    }

    /**
     * Checks whether a file path falls inside any excluded directory for a given project config.
     *
     * Used as a defence-in-depth filter in services that scan files via [FileTypeIndex]
     * with a broad project scope, since those results may include files from excluded directories.
     *
     * @param filePath The absolute path of the file to check
     * @param config The dbt project configuration
     * @return true if the file is inside an excluded directory
     */
    fun isInExcludedDirectory(filePath: String, config: DatamancerProjectConfig): Boolean {
        return isInExcludedDirectory(filePath, config.projectRoot, config.excludedDirectories)
    }

    /**
     * Checks whether a file path falls inside any of the given excluded directories
     * relative to the project root.
     *
     * @param filePath The absolute path of the file to check
     * @param projectRoot The absolute path of the dbt project root
     * @param excludedDirectories Directory names to exclude (relative to project root)
     * @return true if the file is inside an excluded directory
     */
    fun isInExcludedDirectory(
        filePath: String,
        projectRoot: String,
        excludedDirectories: List<String>,
    ): Boolean {
        val root = projectRoot.trimEnd('/')
        return excludedDirectories.any { dirName ->
            filePath.startsWith("$root/${dirName.trim()}/")
        }
    }
}

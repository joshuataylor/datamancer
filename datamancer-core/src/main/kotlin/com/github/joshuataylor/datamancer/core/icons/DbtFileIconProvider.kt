package com.github.joshuataylor.datamancer.core.icons

import com.github.joshuataylor.datamancer.core.services.DatamancerDbtProjectIndexService
import com.intellij.ide.FileIconProvider
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Iconable
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

/**
 * Provides custom icons for dbt SQL files.
 *
 * This provider checks if a SQL file is part of a dbt project and provides
 * a custom icon to visually distinguish dbt SQL files from regular SQL files.
 *
 * Note: The icon used is a test icon and will be replaced with proper design
 * for the final release.
 */
class DbtFileIconProvider : FileIconProvider {
    /**
     * Provides an icon for the given file if it's a dbt SQL file.
     *
     * @param file The virtual file to provide an icon for
     * @param flags Icon flags (unused in this implementation)
     * @param project The current project, or null if no project context
     * @return A custom dbt SQL file icon if the file is a dbt SQL file, or null otherwise
     */
    override fun getIcon(file: VirtualFile, @Iconable.IconFlags flags: Int, project: Project?): Icon? {
        // Only process SQL files
        if (file.extension != "sql") {
            return null
        }

        // Need project context to check for dbt configuration
        if (project == null) {
            return null
        }

        // Find the module containing this file
        val module = ModuleUtil.findModuleForFile(file, project) ?: return null

        // Check if this module has a dbt project configuration
        val indexService = DatamancerDbtProjectIndexService.getInstance(project)
        val dbtConfig = indexService.getDbtConfigForModuleName(module.name) ?: return null

        // Verify the SQL file is actually within the dbt project directory
        // This prevents SQL files outside the dbt project from getting the custom icon
        val projectRoot = dbtConfig.projectRoot
        val filePath = file.path

        if (!filePath.startsWith(projectRoot)) {
            return null
        }

        // File is within dbt project, return custom icon
        return DatamancerIcons.DbtSqlFile
    }
}

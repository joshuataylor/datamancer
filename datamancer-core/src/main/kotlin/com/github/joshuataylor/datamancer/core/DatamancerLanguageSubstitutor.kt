package com.github.joshuataylor.datamancer.core

import com.github.joshuataylor.datamancer.core.services.DatamancerDbtProjectIndexService
import com.intellij.jinja.Jinja2Language
import com.intellij.lang.Language
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.LanguageSubstitutor

/**
 * Language substitutor that treats SQL files in dbt modules as Jinja2 templates.
 * This enables proper syntax highlighting and completion for dbt's Jinja syntax
 * (like {{ ref('model') }}, {{ config() }}, etc.) in SQL files.
 *
 * Uses the DatamancerDbtProjectIndexService's in-memory cache to avoid blocking operations.
 */
class DatamancerLanguageSubstitutor : LanguageSubstitutor() {
    private val log = logger<DatamancerLanguageSubstitutor>()

    override fun getLanguage(file: VirtualFile, project: Project): Language? {
        // Only process SQL files
        if (file.extension != "sql") {
            log.trace("Skipping non-SQL file: ${file.path}")
            return null
        }

        log.debug("Checking if SQL file should use Jinja2 language: ${file.path}")

        // Find the module containing this file
        val module = ModuleUtil.findModuleForFile(file, project)
        if (module == null) {
            log.debug("No module found for file: ${file.path}")
            return null
        }
        log.debug("Found module ${module.name} for file: ${file.path}")

        // Check if this module has a dbt project configuration
        // We use the index service's synchronous access to the in-memory cache
        // This avoids blocking operations since the cache is already populated
        val indexService = DatamancerDbtProjectIndexService.getInstance(project)
        val dbtConfig = indexService.getDbtConfigForModuleName(module.name)

        if (dbtConfig == null) {
            log.debug("No dbt config found, using default language for: ${file.path}")
            return null
        }

        // Verify the SQL file is actually within the dbt project directory
        // This prevents SQL files outside the dbt project from being treated as Jinja2
        val projectRoot = dbtConfig.projectRoot
        val filePath = file.path

        if (!filePath.startsWith(projectRoot)) {
            log.debug("SQL file outside dbt project root, using default language: ${file.path} (project root: $projectRoot)")
            return null
        }

        log.debug("Using Jinja2 language for dbt SQL file: ${file.path}")
        return Jinja2Language.INSTANCE as Language
    }
}

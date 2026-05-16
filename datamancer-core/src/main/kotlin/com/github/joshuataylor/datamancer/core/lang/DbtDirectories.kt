package com.github.joshuataylor.datamancer.core.lang

import com.github.joshuataylor.datamancer.core.services.DatamancerDbtProjectIndexService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.psi.PsiFile
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiManager
import com.intellij.util.concurrency.annotations.RequiresReadLock

/**
 * Utility class for discovering dbt model and seed files within a project.
 *
 * Provides methods to find:
 * - SQL model files in models/ directories
 * - CSV seed files in seeds/ directories
 *
 * Note: dbt SQL files use Jinja2Language via LanguageSubstitutor, so we search
 * by file extension rather than FileType to ensure they are found.
 */
object DbtDirectories {

    /**
     * Finds a dbt model file by name.
     *
     * Searches for `.sql` files in models/ directories across all dbt projects in the workspace.
     * Uses directory traversal since dbt SQL files are treated as Jinja2 templates.
     *
     * @param project The current project
     * @param modelName The name of the model (without .sql extension)
     * @return The PsiFile for the model, or null if not found
     */
    @RequiresReadLock
    fun findModel(project: Project, modelName: String): PsiFile? {
        val indexService = DatamancerDbtProjectIndexService.getInstance(project)
        val allConfigs = indexService.getAllDbtConfigsSync()

        if (allConfigs.isEmpty()) {
            return null
        }

        val psiManager = PsiManager.getInstance(project)
        val targetFileName = "$modelName.sql"
        val vfm = VirtualFileManager.getInstance()

        // Search through each dbt project's models directory
        for ((_, config) in allConfigs) {
            ProgressManager.checkCanceled()
            val modelsPath = "${config.projectRoot}/models"
            val modelsDir = vfm.findFileByUrl("file://$modelsPath") ?: continue

            // Recursively search for the SQL file
            val sqlFile = findFileRecursively(modelsDir, targetFileName)
            if (sqlFile != null) {
                return psiManager.findFile(sqlFile)
            }
        }

        return null
    }

    /**
     * Finds a dbt seed file by name.
     *
     * Searches for `.csv` files in seeds/ directories across all dbt projects in the workspace.
     *
     * @param project The current project
     * @param seedName The name of the seed (without .csv extension)
     * @return The VirtualFile for the seed, or null if not found
     */
    fun findSeedFile(project: Project, seedName: String): VirtualFile? {
        val indexService = DatamancerDbtProjectIndexService.getInstance(project)
        val allConfigs = indexService.getAllDbtConfigsSync()

        if (allConfigs.isEmpty()) {
            return null
        }

        val targetFileName = "$seedName.csv"

        // Search through all dbt project seeds directories
        for ((_, config) in allConfigs) {
            val seedsPath = "${config.projectRoot}/seeds"
            val seedsDir = findDirectory(project, seedsPath) ?: continue

            // Recursively search for the CSV file
            val seedFile = findFileRecursively(seedsDir, targetFileName)
            if (seedFile != null) {
                return seedFile
            }
        }

        return null
    }

    /**
     * Finds all model files in the project.
     *
     * Uses directory traversal since dbt SQL files are treated as Jinja2 templates.
     *
     * @param project The current project
     * @return List of all SQL files in models/ directories
     */
    @RequiresReadLock
    fun findAllModels(project: Project): List<PsiFile> {
        val indexService = DatamancerDbtProjectIndexService.getInstance(project)
        val allConfigs = indexService.getAllDbtConfigsSync()

        if (allConfigs.isEmpty()) {
            return emptyList()
        }

        val psiManager = PsiManager.getInstance(project)
        val models = mutableListOf<PsiFile>()
        val vfm = VirtualFileManager.getInstance()

        for ((_, config) in allConfigs) {
            ProgressManager.checkCanceled()
            val modelsPath = "${config.projectRoot}/models"
            val modelsDir = vfm.findFileByUrl("file://$modelsPath") ?: continue

            val sqlFiles = mutableListOf<VirtualFile>()
            collectSqlFiles(modelsDir, sqlFiles)

            for (virtualFile in sqlFiles) {
                ProgressManager.checkCanceled()
                psiManager.findFile(virtualFile)?.let { models.add(it) }
            }
        }

        return models
    }

    /**
     * Recursively collects all .sql files from a directory.
     */
    private fun collectSqlFiles(directory: VirtualFile, collector: MutableList<VirtualFile>) {
        VfsUtilCore.visitChildrenRecursively(directory, object : VirtualFileVisitor<Void>() {
            override fun visitFile(file: VirtualFile): Boolean {
                if (!file.isDirectory && file.extension == "sql") {
                    collector.add(file)
                }
                return true
            }
        })
    }

    /**
     * Finds all seed files in the project.
     *
     * @param project The current project
     * @return List of all CSV files in seeds/ directories
     */
    fun findAllSeeds(project: Project): List<VirtualFile> {
        val indexService = DatamancerDbtProjectIndexService.getInstance(project)
        val allConfigs = indexService.getAllDbtConfigsSync()

        if (allConfigs.isEmpty()) {
            return emptyList()
        }

        val seeds = mutableListOf<VirtualFile>()

        for ((_, config) in allConfigs) {
            val seedsPath = "${config.projectRoot}/seeds"
            val seedsDir = findDirectory(project, seedsPath) ?: continue

            collectCsvFiles(seedsDir, seeds)
        }

        return seeds
    }

    /**
     * Extracts the model name (without extension) from a file.
     */
    @RequiresReadLock
    fun getModelNameFromFile(file: PsiFile): String {
        return file.virtualFile.nameWithoutExtension
    }

    /**
     * Extracts the seed name (without extension) from a file.
     */
    fun getSeedNameFromFile(file: VirtualFile): String {
        return file.nameWithoutExtension
    }

    // Private helper methods

    private fun findDirectory(project: Project, path: String): VirtualFile? {
        return LocalFileSystem.getInstance().findFileByPath(path)
    }

    private fun findFileRecursively(directory: VirtualFile, fileName: String): VirtualFile? {
        var result: VirtualFile? = null
        VfsUtilCore.visitChildrenRecursively(directory, object : VirtualFileVisitor<Void>() {
            override fun visitFile(file: VirtualFile): Boolean {
                if (!file.isDirectory && file.name == fileName) {
                    result = file
                    return false
                }
                return true
            }
        })
        return result
    }

    private fun collectCsvFiles(directory: VirtualFile, collector: MutableList<VirtualFile>) {
        VfsUtilCore.visitChildrenRecursively(directory, object : VirtualFileVisitor<Void>() {
            override fun visitFile(file: VirtualFile): Boolean {
                if (!file.isDirectory && file.extension == "csv") {
                    collector.add(file)
                }
                return true
            }
        })
    }
}

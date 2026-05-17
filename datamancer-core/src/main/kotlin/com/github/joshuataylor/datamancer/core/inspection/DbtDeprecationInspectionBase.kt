package com.github.joshuataylor.datamancer.core.inspection

import com.github.joshuataylor.datamancer.core.services.DatamancerDbtProjectIndexService
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YamlPsiElementVisitor

/**
 * Abstract base class for dbt deprecation inspections.
 *
 * Provides common logic for checking whether a file belongs to a dbt project
 * and restricting inspection scope to appropriate file types.
 */
abstract class DbtDeprecationInspectionBase : LocalInspectionTool(), DumbAware {

    override fun getGroupDisplayName(): String = "dbt"

    /**
     * Subclasses implement this to visit YAML elements within confirmed dbt project files.
     */
    abstract fun buildYamlVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): YamlPsiElementVisitor

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        val file = holder.file
        if (file !is YAMLFile) return PsiElementVisitor.EMPTY_VISITOR
        if (!isInDbtProject(file)) return PsiElementVisitor.EMPTY_VISITOR
        if (!isApplicableFile(file)) return PsiElementVisitor.EMPTY_VISITOR
        return buildYamlVisitor(holder, isOnTheFly)
    }

    /**
     * Whether this inspection applies to the given file.
     * Override to restrict to specific filenames (e.g. dbt_project.yml only).
     */
    protected open fun isApplicableFile(file: YAMLFile): Boolean = true

    /**
     * Checks whether the file resides within a known dbt project root.
     * Uses the in-memory project index (ConcurrentHashMap lookup, thread-safe).
     */
    protected fun isInDbtProject(file: PsiFile): Boolean {
        val vFile = file.virtualFile ?: return false
        val indexService = DatamancerDbtProjectIndexService.getInstance(file.project)
        val filePath = vFile.path
        return indexService.getAllDbtConfigsSync().values.any { config ->
            filePath.startsWith(config.projectRoot.trimEnd('/') + "/")
        }
    }

    /**
     * Checks whether the file is a dbt_project.yml file.
     */
    protected fun isDbtProjectYml(file: YAMLFile): Boolean {
        val name = file.virtualFile?.name ?: return false
        return name == "dbt_project.yml" || name == "dbt_project.yaml"
    }
}

package com.github.joshuataylor.datamancer.core.projectView

import com.github.joshuataylor.datamancer.core.Icons
import com.github.joshuataylor.datamancer.core.services.DatamancerDbtProjectIndexService
import com.github.joshuataylor.datamancer.core.workspace.DatamancerProjectConfigStore
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ProjectViewNodeDecorator
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.openapi.diagnostic.logger

/**
 * Decorates project view nodes to show custom icons for dbt project root directories.
 *
 * This decorator uses the DatamancerDbtProjectIndexService's in-memory cache to avoid blocking operations.
 * The cache is maintained by the index service and updated when the workspace model changes.
 */
class DatamancerDbtProjectViewNodeDecorator : ProjectViewNodeDecorator {
    private val log = logger<DatamancerDbtProjectViewNodeDecorator>()

    override fun decorate(node: ProjectViewNode<*>, data: PresentationData) {
        // Only decorate directory nodes
        if (node !is PsiDirectoryNode) return

        val virtualFile = node.virtualFile ?: return
        val project = node.project ?: return

        // Check if project icon display is enabled in settings
        if (!DatamancerProjectConfigStore.getInstance(project).showDbtProjectIcon) return

        val dirPath = virtualFile.path
        log.trace("Checking if directory is dbt project root: $dirPath")

        // Check if this directory is a dbt project root
        // We use the index service's synchronous access to the in-memory cache
        // This avoids any blocking operations since the cache is already populated by the startup activity
        val isDbtRoot = try {
            val indexService = DatamancerDbtProjectIndexService.getInstance(project)

            // Check the in-memory cache synchronously
            // The index service maintains a ConcurrentHashMap that's safe to access from any thread
            val isDbt = indexService.isDbtProjectRoot(dirPath)

            if (isDbt) {
                log.debug("Directory is dbt project root: $dirPath")
            } else if (log.isDebugEnabled) {
                val allRoots = indexService.getAllDbtConfigsSync().values.map { it.projectRoot }
                if (allRoots.isNotEmpty()) {
                    log.debug("Directory $dirPath is not a dbt project root. Known roots: $allRoots")
                }
            }
            isDbt
        } catch (e: Exception) {
            // Fail silently if index is not ready or project is disposed
            log.debug("Error checking if directory is dbt project root: $dirPath", e)
            false
        }

        if (isDbtRoot) {
            // Set the dbt project icon
            log.trace("Setting dbt project icon for: $dirPath")
            data.setIcon(Icons.PROJECT_ROOT)
        }
    }
}

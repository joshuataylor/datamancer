package com.github.joshuataylor.datamancer.dbtcore.manifest

import com.github.joshuataylor.datamancer.core.services.DatamancerDbtProjectIndexService
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.AsyncFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent

/**
 * Async VFS listener that reloads manifest.json whenever it changes or is created on disk.
 *
 * Watches for events on files named `manifest.json` inside a `target/` directory that
 * belongs to a known dbt project root. On a match, triggers a reload of the cached manifest
 * in [DatamancerManifestService].
 */
class DatamancerManifestFileListener : AsyncFileListener {
    private val log = logger<DatamancerManifestFileListener>()

    override fun prepareChange(events: List<VFileEvent>): AsyncFileListener.ChangeApplier? {
        val relevantPaths = events
            .filter { event ->
                (event is VFileContentChangeEvent || event is VFileCreateEvent) &&
                    event.file?.name == "manifest.json" &&
                    event.file?.parent?.name == "target"
            }
            .mapNotNull { it.file?.parent?.parent?.path }
            .distinct()

        if (relevantPaths.isEmpty()) return null

        return object : AsyncFileListener.ChangeApplier {
            override fun afterVfsChange() {
                for (projectRoot in relevantPaths) {
                    // Find open projects that have this path as a dbt project root
                    for (project in ProjectManager.getInstance().openProjects) {
                        if (project.isDisposed) continue
                        val indexService = DatamancerDbtProjectIndexService.getInstance(project)
                        val configs = indexService.getAllDbtConfigsSync()
                        val isKnown = configs.values.any { it.projectRoot == projectRoot }
                        if (isKnown) {
                            log.debug("manifest.json changed in $projectRoot, reloading")
                            DatamancerManifestService.getInstance(project).reloadManifest(projectRoot)
                        }
                    }
                }
            }
        }
    }
}

package com.github.joshuataylor.datamancer.core.settings

import com.github.joshuataylor.datamancer.core.services.DatamancerDbtProjectIndexService
import com.github.joshuataylor.datamancer.core.services.DatamancerProjectDiscoveryService
import com.github.joshuataylor.datamancer.core.workspace.DatamancerExcludedDirectories
import com.github.joshuataylor.datamancer.core.workspace.DatamancerProjectConfig
import com.github.joshuataylor.datamancer.core.workspace.DatamancerProjectConfigStore
import com.github.joshuataylor.datamancer.core.workspace.setDbtConfig
import com.intellij.database.dataSource.LocalDataSourceManager
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.execution.configuration.EnvironmentVariablesComponent
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.readAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import kotlinx.coroutines.CancellationException
import javax.swing.DefaultComboBoxModel

/**
 * Settings configurable for viewing and managing dbt projects.
 */
class ProjectSettingsConfigurable(private val project: Project) : BoundSearchableConfigurable(
    displayName = "dbt Projects",
    helpTopic = "reference.settings.dbt",
    _id = "com.github.joshuataylor.datamancer.core.settings"
) {
    private val log = logger<ProjectSettingsConfigurable>()

    // Store original and modified configurations
    private var originalConfigs: Map<String, DatamancerProjectConfig> = emptyMap()
    private val modifiedConfigs: MutableMap<String, DatamancerProjectConfig> = mutableMapOf()

    // UI components
    private val showProjectIconCheckbox = javax.swing.JCheckBox("Show dbt project icon in project view").apply {
        toolTipText = "Display a custom icon on directories that are dbt project roots"
    }
    private val showRunGutterIconCheckbox = javax.swing.JCheckBox("Show run gutter icon for dbt models").apply {
        toolTipText = "Display a run icon in the editor gutter for dbt model SQL files"
    }
    private val projectSelector: ComboBox<String> = ComboBox<String>()
    private val profileDirectoryField = JBTextField()
    private val defaultTargetField = JBTextField()
    private val dataSourceCombo: ComboBox<String> = ComboBox<String>()
    private val queryLimitField = JBTextField()
    private val envVarsComponent = EnvironmentVariablesComponent()
    private val excludedDirectoriesField = JBTextField()
    private val useMiseCheckbox = javax.swing.JCheckBox("Use mise environment variables").apply {
        toolTipText = "Inject environment variables from mise when compiling dbt models"
    }

    private val misePluginAvailable: Boolean by lazy {
        val pluginId = PluginId.findId("com.github.l34130.mise") ?: return@lazy false
        val descriptor = PluginManagerCore.getPlugin(pluginId) ?: return@lazy false
        descriptor.isEnabled
    }

    private var panel: DialogPanel? = null
    private var initialised = false

    private fun loadGeneralSettings() {
        val store = DatamancerProjectConfigStore.getInstance(project)
        showProjectIconCheckbox.isSelected = store.showDbtProjectIcon
        showRunGutterIconCheckbox.isSelected = store.showRunGutterIcon
    }

    private fun loadConfigs() {
        log.debug("Loading dbt project configurations")
        val indexService = DatamancerDbtProjectIndexService.getInstance(project)

        // Use the synchronous method to get all configs from the in-memory cache
        // This avoids blocking operations since the cache is maintained by the index service
        originalConfigs = try {
            val projects = indexService.getAllDbtConfigsSync()
            log.debug("Loaded ${projects.size} dbt project configurations")
            projects
        } catch (e: Exception) {
            log.error("Error loading dbt project configurations", e)
            emptyMap()
        }
        modifiedConfigs.clear()
        modifiedConfigs.putAll(originalConfigs)
        log.debug("Initialised modified configs with ${modifiedConfigs.size} entries")
    }

    private fun setupDataSourceCombo() {
        log.debug("Setting up data source combo box")
        val dataSources = try {
            val sources = LocalDataSourceManager.getInstance(project).dataSources.map { it.name }
            log.debug("Found ${sources.size} data sources")
            sources
        } catch (e: Exception) {
            log.error("Error loading data sources", e)
            emptyList()
        }
        dataSourceCombo.model = DefaultComboBoxModel((listOf("<none>") + dataSources).toTypedArray())
    }

    override fun createPanel(): DialogPanel {
        // Lazy initialisation - only load when panel is actually created
        if (!initialised) {
            loadConfigs()
            loadGeneralSettings()
            setupDataSourceCombo()
            initialised = true
        }

        return panel {
            group("Appearance") {
                row {
                    cell(showProjectIconCheckbox)
                }
                row {
                    cell(showRunGutterIconCheckbox)
                }
            }

            group("dbt Projects") {
                row {
                    comment("Configure settings for each discovered dbt project")
                }

                if (modifiedConfigs.isEmpty()) {
                    row {
                        label("No dbt projects discovered. Click 'Refresh Projects' to scan.")
                            .applyToComponent {
                                foreground = com.intellij.ui.JBColor.GRAY
                            }
                    }
                } else {
                    row("Select Project:") {
                        cell(projectSelector)
                            .align(AlignX.FILL)
                            .applyToComponent {
                                model = DefaultComboBoxModel(modifiedConfigs.keys.toTypedArray())
                                addActionListener {
                                    updateFormFromSelectedProject()
                                }
                                // Select first project by default
                                if (modifiedConfigs.isNotEmpty()) {
                                    selectedIndex = 0
                                    updateFormFromSelectedProject()
                                }
                            }
                    }

                    separator()

                    row("Project Root:") {
                        label("")
                            .applyToComponent {
                                val selectedProject = projectSelector.selectedItem as? String
                                text = modifiedConfigs[selectedProject]?.projectRoot ?: ""
                            }
                            .component
                    }

                    row("Profile Directory:") {
                        cell(profileDirectoryField)
                            .align(AlignX.FILL)
                            .comment("Optional: Path to dbt profiles directory")
                    }

                    row("Default Target:") {
                        cell(defaultTargetField)
                            .align(AlignX.FILL)
                            .comment("Optional: Default dbt target (e.g., dev, prod)")
                    }

                    row("Default Data Source:") {
                        cell(dataSourceCombo)
                            .align(AlignX.FILL)
                            .comment("Database connection to use for queries")
                    }

                    row("Query Limit:") {
                        cell(queryLimitField)
                            .columns(10)
                            .comment("Default row limit for query results")
                            .validationOnApply {
                                val value = it.text.toIntOrNull()
                                if (value == null || value <= 0) {
                                    error("Must be a positive integer")
                                } else {
                                    null
                                }
                            }
                    }

                    row {
                        cell(envVarsComponent)
                            .align(AlignX.FILL)
                    }.layout(RowLayout.PARENT_GRID)

                    row("Excluded Directories:") {
                        cell(excludedDirectoriesField)
                            .align(AlignX.FILL)
                            .comment("Comma-separated directory names to exclude from indexing (relative to project root)")
                    }

                    if (misePluginAvailable) {
                        row {
                            cell(useMiseCheckbox)
                                .comment("Requires the Mise plugin to be installed and enabled")
                        }
                    }
                }

                row {
                    button("Refresh Projects") {
                        log.debug("Refresh Projects button clicked")
                        var discoveredCount = 0
                        var error: Exception? = null
                        runWithModalProgressBlocking(project, "Discovering dbt Projects") {
                            try {
                                log.debug("Starting dbt project discovery")
                                val discoveryService = DatamancerProjectDiscoveryService.getInstance(project)
                                discoveryService.discoverAndAssociate()
                                log.debug("Discovery completed successfully")
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                log.error("Error during dbt project discovery", e)
                                error = e
                            }
                        }
                        loadConfigs()
                        discoveredCount = modifiedConfigs.size
                        reset()

                        val notificationGroup = NotificationGroupManager.getInstance()
                            .getNotificationGroup("Datamancer")
                        val settingsAction = NotificationAction.createSimpleExpiring("Open Settings") {
                            ShowSettingsUtil.getInstance().showSettingsDialog(
                                project,
                                "com.github.joshuataylor.datamancer.settings"
                            )
                        }
                        if (error != null) {
                            notificationGroup.createNotification(
                                "dbt Project Discovery",
                                "Discovery failed: ${error.message}",
                                NotificationType.ERROR
                            ).addAction(settingsAction).notify(project)
                        } else {
                            notificationGroup.createNotification(
                                "dbt Project Discovery",
                                "Found $discoveredCount dbt project(s)",
                                NotificationType.INFORMATION
                            ).addAction(settingsAction).notify(project)
                        }
                    }
                    comment("Re-scan the project for dbt_project.yml files")
                }
            }
        }.also { panel = it }
    }

    private fun updateFormFromSelectedProject() {
        val selectedProject = projectSelector.selectedItem as? String ?: return
        log.debug("Updating form for selected project: $selectedProject")
        val config = modifiedConfigs[selectedProject] ?: return

        profileDirectoryField.text = config.profileDirectory ?: ""
        defaultTargetField.text = config.defaultTarget ?: ""
        dataSourceCombo.selectedItem = config.defaultDataSource ?: "<none>"
        queryLimitField.text = config.queryLimit.toString()
        envVarsComponent.envData = EnvironmentVariablesData.create(config.environmentVariables, true)
        excludedDirectoriesField.text = config.excludedDirectories.joinToString(", ")
        if (misePluginAvailable) {
            useMiseCheckbox.isSelected = config.useMiseEnvironment
        }
        log.debug("Form updated for project: $selectedProject")
    }

    private fun saveFormToSelectedProject() {
        val selectedProject = projectSelector.selectedItem as? String ?: return
        log.debug("Saving form to project: $selectedProject")
        val currentConfig = modifiedConfigs[selectedProject] ?: return

        val dataSource = (dataSourceCombo.selectedItem as? String)?.takeIf { it != "<none>" }

        val excludedDirs = excludedDirectoriesField.text
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val updatedConfig = currentConfig.copy(
            profileDirectory = profileDirectoryField.text.takeIf { it.isNotBlank() },
            defaultTarget = defaultTargetField.text.takeIf { it.isNotBlank() },
            defaultDataSource = dataSource,
            queryLimit = queryLimitField.text.toIntOrNull() ?: 1000,
            environmentVariables = envVarsComponent.envData.envs,
            useMiseEnvironment = if (misePluginAvailable) useMiseCheckbox.isSelected else currentConfig.useMiseEnvironment,
            excludedDirectories = excludedDirs
        )

        modifiedConfigs[selectedProject] = updatedConfig
        log.debug("Saved configuration for project: $selectedProject")
    }

    override fun apply() {
        log.debug("Applying dbt project settings")

        // Save general settings
        val store = DatamancerProjectConfigStore.getInstance(project)
        val iconSettingChanged = store.showDbtProjectIcon != showProjectIconCheckbox.isSelected
        val gutterSettingChanged = store.showRunGutterIcon != showRunGutterIconCheckbox.isSelected
        store.showDbtProjectIcon = showProjectIconCheckbox.isSelected
        store.showRunGutterIcon = showRunGutterIconCheckbox.isSelected

        // Save current form to selected project
        saveFormToSelectedProject()

        // Apply all modified configs to workspace model using proper coroutine pattern
        runWithModalProgressBlocking(project, "Applying dbt Settings") {
            var appliedCount = 0
            modifiedConfigs.forEach { (moduleName, config) ->
                log.debug("Applying configuration for module: $moduleName")

                // Access workspace model in read action
                val moduleEntity = readAction {
                    val workspaceModel = com.intellij.platform.backend.workspace.WorkspaceModel.getInstance(project)
                    val snapshot = workspaceModel.currentSnapshot
                    val moduleId = ModuleId(moduleName)
                    moduleId.resolve(snapshot)
                }

                if (moduleEntity != null) {
                    project.setDbtConfig(moduleEntity, config)
                    appliedCount++
                    log.debug("Applied configuration for module: $moduleName")
                } else {
                    log.warn("Could not find module entity for: $moduleName")
                }
            }
            log.debug("Applied ${appliedCount} dbt project configurations")
        }

        // Re-apply directory exclusions for any projects where the list changed
        for ((moduleName, config) in modifiedConfigs) {
            val previousConfig = originalConfigs[moduleName]
            if (previousConfig == null || previousConfig.excludedDirectories != config.excludedDirectories) {
                log.debug("Excluded directories changed for module '$moduleName', re-applying exclusions")
                DatamancerExcludedDirectories.applyExclusions(project, moduleName, config)
            }
        }

        // Update original configs to reflect applied changes
        originalConfigs = modifiedConfigs.toMap()
        log.debug("Updated original configs after apply")

        // Refresh project view if icon setting changed
        if (iconSettingChanged) {
            ProjectView.getInstance(project).currentProjectViewPane?.updateFromRoot(true)
        }

        // Restart daemon analysis to refresh run gutter icons in open editors
        if (gutterSettingChanged) {
            com.intellij.codeInsight.daemon.DaemonCodeAnalyzer.getInstance(project).restart()
        }
    }

    override fun reset() {
        log.debug("Resetting dbt project settings")
        loadGeneralSettings()
        loadConfigs()
        panel?.reset()
        if (modifiedConfigs.isNotEmpty()) {
            projectSelector.selectedIndex = 0
            updateFormFromSelectedProject()
            log.debug("Form reset to first project")
        } else {
            log.debug("No projects to reset")
        }
    }

    override fun isModified(): Boolean {
        // Check general settings
        val store = DatamancerProjectConfigStore.getInstance(project)
        if (store.showDbtProjectIcon != showProjectIconCheckbox.isSelected) return true
        if (store.showRunGutterIcon != showRunGutterIconCheckbox.isSelected) return true

        // Save current form changes before checking
        if (modifiedConfigs.isNotEmpty()) {
            saveFormToSelectedProject()
        }
        val modified = modifiedConfigs != originalConfigs
        if (modified) {
            log.debug("Settings have been modified")
        } else {
            log.trace("Settings not modified")
        }
        return modified
    }
}

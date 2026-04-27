package com.github.joshuataylor.datamancer.core.workspace

import com.intellij.openapi.components.*
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.annotations.MapAnnotation
import com.intellij.util.xmlb.annotations.Tag

/**
 * Persistent store for dbt project configurations.
 *
 * External mappings on [ModuleEntity][com.intellij.platform.workspace.jps.entities.ModuleEntity]
 * are in-memory only and lost on project close. This component persists the same data to
 * `workspace.xml` so configs survive across sessions.
 *
 * At startup, [DatamancerProjectDiscoveryService][com.github.joshuataylor.datamancer.core.services.DatamancerProjectDiscoveryService]
 * restores saved configs into external mappings.
 */
@Service(Service.Level.PROJECT)
@State(
    name = "DatamancerProjectConfigStore",
    storages = [Storage(StoragePathMacros.WORKSPACE_FILE)]
)
class DatamancerProjectConfigStore : PersistentStateComponent<DatamancerProjectConfigStore.State> {
    private val log = logger<DatamancerProjectConfigStore>()

    /**
     * JavaBean-style config for IntelliJ XML serialisation.
     * Mirrors [DatamancerProjectConfig] but with mutable fields and no-arg constructor.
     */
    @Tag("config")
    data class PersistedProjectConfig(
        var projectRoot: String = "",
        var profileDirectory: String? = null,
        var defaultTarget: String? = null,
        var defaultDataSource: String? = null,
        var queryLimit: Int = 1000,
        @MapAnnotation(entryTagName = "var", keyAttributeName = "name", valueAttributeName = "value")
        var environmentVariables: MutableMap<String, String> = mutableMapOf(),
        var preferredBackend: String? = null,
        var excludedDirectories: MutableList<String> = DatamancerProjectConfig.DEFAULT_EXCLUDED_DIRECTORIES.toMutableList(),
    )

    data class State(
        @MapAnnotation(entryTagName = "project", keyAttributeName = "module")
        var configs: MutableMap<String, PersistedProjectConfig> = mutableMapOf(),
        var showDbtProjectIcon: Boolean = true,
        var showRunGutterIcon: Boolean = true,
    )

    private var myState = State()

    var showDbtProjectIcon: Boolean
        get() = myState.showDbtProjectIcon
        set(value) {
            myState.showDbtProjectIcon = value
        }

    var showRunGutterIcon: Boolean
        get() = myState.showRunGutterIcon
        set(value) {
            myState.showRunGutterIcon = value
        }

    override fun getState(): State = myState

    override fun loadState(state: State) {
        log.debug("Loading persisted configs: ${state.configs.size} projects")
        myState = state
    }

    fun getConfig(moduleName: String): DatamancerProjectConfig? {
        val persisted = myState.configs[moduleName] ?: return null
        return persisted.toProjectConfig()
    }

    fun getAllConfigs(): Map<String, DatamancerProjectConfig> {
        return myState.configs.mapValues { it.value.toProjectConfig() }
    }

    fun saveConfig(moduleName: String, config: DatamancerProjectConfig) {
        log.debug("Saving config for module: $moduleName (dataSource=${config.defaultDataSource})")
        myState.configs[moduleName] = config.toPersisted()
    }

    fun removeConfig(moduleName: String) {
        log.debug("Removing config for module: $moduleName")
        myState.configs.remove(moduleName)
    }

    companion object {
        fun getInstance(project: Project): DatamancerProjectConfigStore =
            project.getService(DatamancerProjectConfigStore::class.java)
    }
}

private fun DatamancerProjectConfigStore.PersistedProjectConfig.toProjectConfig() = DatamancerProjectConfig(
    projectRoot = projectRoot,
    profileDirectory = profileDirectory,
    defaultTarget = defaultTarget,
    defaultDataSource = defaultDataSource,
    queryLimit = queryLimit,
    environmentVariables = environmentVariables.toMap(),
    preferredBackend = preferredBackend,
    excludedDirectories = excludedDirectories.toList(),
)

private fun DatamancerProjectConfig.toPersisted() = DatamancerProjectConfigStore.PersistedProjectConfig(
    projectRoot = projectRoot,
    profileDirectory = profileDirectory,
    defaultTarget = defaultTarget,
    defaultDataSource = defaultDataSource,
    queryLimit = queryLimit,
    environmentVariables = environmentVariables.toMutableMap(),
    preferredBackend = preferredBackend,
    excludedDirectories = excludedDirectories.toMutableList(),
)

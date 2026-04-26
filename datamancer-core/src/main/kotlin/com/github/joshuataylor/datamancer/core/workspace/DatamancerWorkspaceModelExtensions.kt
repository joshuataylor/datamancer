package com.github.joshuataylor.datamancer.core.workspace

import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.platform.workspace.storage.ImmutableEntityStorage
import com.intellij.workspaceModel.ide.legacyBridge.ModuleBridge

private val log = logger<Module>()

/**
 * Get the ModuleEntity from a Module (legacy bridge).
 * This function accesses the workspace model which requires read access.
 */
suspend fun Module.getModuleEntity(): ModuleEntity? {
    log.debug("Getting module entity for module: ${this.name}")
    return readAction {
        val moduleBridge = this as? ModuleBridge
        if (moduleBridge == null) {
            log.warn("Module is not a ModuleBridge: ${this.name}")
            return@readAction null
        }

        val storage = moduleBridge.entityStorage.current
        val moduleEntity = storage.entities(ModuleEntity::class.java).find { it.name == this.name }
        if (moduleEntity != null) {
            log.debug("Found module entity for module: ${this.name}")
        } else {
            log.warn("Module entity not found for module: ${this.name}")
        }
        moduleEntity
    }
}

/**
 * Get the dbt configuration for this module entity.
 * The storage parameter should come from a readAction context.
 */
fun ModuleEntity.getDbtConfig(storage: ImmutableEntityStorage): DatamancerProjectConfig? {
    log.debug("Getting dbt config for module: ${this.name}")
    val mapping = storage.getExternalMapping(DatamancerWorkspaceKeys.DBT_PROJECT_CONFIG)
    val config = mapping.getDataByEntity(this)
    if (config != null) {
        log.debug("Found dbt config for module: ${this.name}, project root: ${config.projectRoot}")
    } else {
        log.debug("No dbt config found for module: ${this.name}")
    }
    return config
}

/**
 * Get all modules with dbt configurations.
 * Uses readAction to ensure proper read lock acquisition.
 */
suspend fun Project.getAllDbtModules(): List<Pair<ModuleEntity, DatamancerProjectConfig>> {
    log.debug("Getting all dbt modules for project: ${this.name}")
    return readAction {
        val workspaceModel = WorkspaceModel.getInstance(this@getAllDbtModules)
        val snapshot = workspaceModel.currentSnapshot
        val mapping = snapshot.getExternalMapping(DatamancerWorkspaceKeys.DBT_PROJECT_CONFIG)

        val modules = snapshot.entities(ModuleEntity::class.java).mapNotNull { module ->
            mapping.getDataByEntity(module)?.let { config ->
                log.debug("Found dbt module: ${module.name} with project root: ${config.projectRoot}")
                module to config
            }
        }.toList()

        log.debug("Found ${modules.size} dbt modules in project: ${this@getAllDbtModules.name}")
        modules
    }
}

/**
 * Update or add a dbt configuration for a module.
 * Writes to both the in-memory external mapping and the persistent store.
 */
suspend fun Project.setDbtConfig(moduleEntity: ModuleEntity, config: DatamancerProjectConfig) {
    log.debug("Setting dbt config for module: ${moduleEntity.name}, project root: ${config.projectRoot}")
    val moduleName = moduleEntity.name
    val workspaceModel = WorkspaceModel.getInstance(this)
    // Use writeAction + updateProjectModel because the suspend update() silently discards
    // external-mapping-only changes (areEntitiesChanged returns false).
    writeAction {
        workspaceModel.updateProjectModel("Update dbt configuration") { builder ->
            val module = ModuleId(moduleName).resolve(builder) ?: return@updateProjectModel
            val mapping = builder.getMutableExternalMapping(DatamancerWorkspaceKeys.DBT_PROJECT_CONFIG)
            mapping.addMapping(module, config)
        }
    }
    // Persist to workspace.xml so config survives project close/reopen
    DatamancerProjectConfigStore.getInstance(this).saveConfig(moduleName, config)
    log.debug("Successfully set dbt config for module: $moduleName")
}

/**
 * Remove dbt configuration for a module.
 * Removes from both the in-memory external mapping and the persistent store.
 */
suspend fun Project.removeDbtConfig(moduleEntity: ModuleEntity) {
    log.debug("Removing dbt config for module: ${moduleEntity.name}")
    val moduleName = moduleEntity.name
    val workspaceModel = WorkspaceModel.getInstance(this)
    writeAction {
        workspaceModel.updateProjectModel("Remove dbt configuration") { builder ->
            val module = ModuleId(moduleName).resolve(builder) ?: return@updateProjectModel
            val mapping = builder.getMutableExternalMapping(DatamancerWorkspaceKeys.DBT_PROJECT_CONFIG)
            mapping.removeMapping(module)
        }
    }
    DatamancerProjectConfigStore.getInstance(this).removeConfig(moduleName)
    log.debug("Successfully removed dbt config for module: $moduleName")
}

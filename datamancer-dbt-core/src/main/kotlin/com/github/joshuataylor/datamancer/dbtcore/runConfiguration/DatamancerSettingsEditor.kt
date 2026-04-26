package com.github.joshuataylor.datamancer.dbtcore.runConfiguration

import com.github.joshuataylor.datamancer.dbtcore.mise.DatamancerMiseIntegration
import com.github.joshuataylor.datamancer.core.services.DatamancerDbtProjectIndexService
import com.intellij.database.dataSource.LocalDataSourceManager
import com.intellij.execution.configuration.EnvironmentVariablesComponent
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Settings editor for the dbt compile and run configuration.
 * Provides UI for configuring model name, module, data source, query limit, and environment variables.
 */
class DatamancerSettingsEditor(private val project: Project) : SettingsEditor<DatamancerRunConfiguration>() {

    private val modelNameComponent = JBTextField().apply {
        emptyText.text = "e.g., my_model or path/to/my_model"
    }

    private val moduleComponent: ComboBox<String> = ComboBox<String>().apply {
        // Populate with modules that have dbt projects
        val indexService = DatamancerDbtProjectIndexService.getInstance(project)
        val dbtModules = indexService.getAllDbtConfigsSync().keys.sorted()
        addItem("") // Empty option
        dbtModules.forEach { addItem(it) }
    }

    private val dataSourceComponent: ComboBox<String> = ComboBox<String>().apply {
        // Populate with available data sources
        addItem("") // Empty means use project default
        LocalDataSourceManager.getInstance(project).dataSources.forEach {
            addItem(it.name)
        }
    }

    private val queryLimitComponent = JBTextField().apply {
        emptyText.text = "Leave blank for project default (1000)"
    }

    private val envVarsComponent = EnvironmentVariablesComponent()

    private val miseAvailable = DatamancerMiseIntegration.isMisePluginAvailable()

    private val useMiseCheckbox = JCheckBox("Use mise environment variables").apply {
        toolTipText = "Inject environment variables from mise when compiling dbt models"
    }

    private val panel: JPanel = FormBuilder.createFormBuilder()
        .addLabeledComponent("Model name:", modelNameComponent)
        .addLabeledComponent("dbt Project (Module):", moduleComponent)
        .addLabeledComponent("Data source:", dataSourceComponent)
        .addLabeledComponent("Query limit:", queryLimitComponent)
        .addSeparator()
        .apply {
            if (miseAvailable) {
                addComponent(useMiseCheckbox)
            }
        }
        .addComponent(envVarsComponent)
        .addComponentFillVertically(JPanel(), 0)
        .panel

    override fun resetEditorFrom(configuration: DatamancerRunConfiguration) {
        modelNameComponent.text = configuration.modelName ?: ""
        moduleComponent.selectedItem = configuration.moduleName ?: ""
        dataSourceComponent.selectedItem = configuration.dataSourceName ?: ""
        queryLimitComponent.text = configuration.queryLimit?.toString() ?: ""
        envVarsComponent.envData = EnvironmentVariablesData.create(
            configuration.environmentVariables,
            true
        )
        if (miseAvailable) {
            useMiseCheckbox.isSelected = configuration.useMiseEnvironment
        }
    }

    override fun applyEditorTo(configuration: DatamancerRunConfiguration) {
        configuration.modelName = modelNameComponent.text.takeIf { it.isNotBlank() }
        configuration.moduleName = (moduleComponent.selectedItem as? String)?.takeIf { it.isNotBlank() }
        configuration.dataSourceName = (dataSourceComponent.selectedItem as? String)?.takeIf { it.isNotBlank() }
        configuration.queryLimit = queryLimitComponent.text.toIntOrNull()
        configuration.environmentVariables = envVarsComponent.envData.envs.toMutableMap()
        if (miseAvailable) {
            configuration.useMiseEnvironment = useMiseCheckbox.isSelected
        }
    }

    override fun createEditor(): JComponent = panel
}

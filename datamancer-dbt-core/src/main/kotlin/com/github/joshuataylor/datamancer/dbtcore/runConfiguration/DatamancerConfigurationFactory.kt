package com.github.joshuataylor.datamancer.dbtcore.runConfiguration

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.project.Project

/**
 * Factory for creating dbt compile and run configurations.
 */
class DatamancerConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {

    override fun getId(): String = DatamancerRunConfigurationType.ID

    override fun createTemplateConfiguration(project: Project): RunConfiguration =
        DatamancerRunConfiguration(project, this, "dbt Compile & Run")

    override fun getOptionsClass(): Class<out BaseState?> =
        DatamancerRunConfigurationOptions::class.java
}

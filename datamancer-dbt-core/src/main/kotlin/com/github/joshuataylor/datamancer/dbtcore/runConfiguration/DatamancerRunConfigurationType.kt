package com.github.joshuataylor.datamancer.dbtcore.runConfiguration

import com.github.joshuataylor.datamancer.core.Icons
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.openapi.util.NotNullLazyValue

/**
 * Run configuration type for compiling and executing dbt models.
 * Registered via plugin.xml as a configurationType extension.
 */
internal class DatamancerRunConfigurationType : ConfigurationTypeBase(
    ID,
    "dbt Compile & Run",
    "Compile a dbt model and execute the SQL in the database console",
    NotNullLazyValue.createValue { Icons.PROJECT_ROOT },
) {
    init {
        addFactory(DatamancerConfigurationFactory(this))
    }

    companion object {
        const val ID: String = "DatamancerRunConfiguration"
    }
}

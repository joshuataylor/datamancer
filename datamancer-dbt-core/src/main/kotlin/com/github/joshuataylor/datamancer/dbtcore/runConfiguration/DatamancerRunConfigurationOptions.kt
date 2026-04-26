package com.github.joshuataylor.datamancer.dbtcore.runConfiguration

import com.intellij.execution.configurations.RunConfigurationOptions
import com.intellij.openapi.components.StoredProperty

/**
 * Persisted options for the dbt model compile and run configuration.
 */
class DatamancerRunConfigurationOptions : RunConfigurationOptions() {
    /**
     * The name of the dbt model to compile (e.g., "my_model").
     */
    private val myModelName: StoredProperty<String?> =
        string("").provideDelegate(this, "modelName")

    /**
     * The name of the module containing the dbt project.
     */
    private val myModuleName: StoredProperty<String?> =
        string("").provideDelegate(this, "moduleName")

    /**
     * The name of the data source to use for query execution.
     * If null or empty, uses the default from the dbt project configuration.
     */
    private val myDataSourceName: StoredProperty<String?> =
        string("").provideDelegate(this, "dataSourceName")

    /**
     * Query row limit. Wraps the compiled SQL in `SELECT * FROM (...) LIMIT n`.
     * If empty or 0, uses the default from the dbt project configuration.
     * Stored as String to allow empty/null state.
     */
    private val myQueryLimit: StoredProperty<String?> =
        string("").provideDelegate(this, "queryLimit")

    /**
     * Environment variables to set when running the dbt compile command.
     * These are merged with the environment variables from the dbt project configuration.
     */
    private val myEnvironmentVariables: StoredProperty<MutableMap<String, String>> =
        map<String, String>().provideDelegate(this, "environmentVariables")

    /**
     * Whether to inject mise environment variables when compiling.
     * Only effective when the mise plugin (com.github.l34130.mise) is installed.
     * Defaults to true so mise is used automatically when available.
     */
    private val myUseMiseEnvironment: StoredProperty<Boolean> =
        property(true).provideDelegate(this, "useMiseEnvironment")

    var modelName: String?
        get() = myModelName.getValue(this)
        set(value) {
            myModelName.setValue(this, value)
        }

    var moduleName: String?
        get() = myModuleName.getValue(this)
        set(value) {
            myModuleName.setValue(this, value)
        }

    var dataSourceName: String?
        get() = myDataSourceName.getValue(this)
        set(value) {
            myDataSourceName.setValue(this, value)
        }

    var queryLimit: Int?
        get() = myQueryLimit.getValue(this)?.toIntOrNull()
        set(value) {
            myQueryLimit.setValue(this, value?.toString() ?: "")
        }

    var environmentVariables: MutableMap<String, String>
        get() = myEnvironmentVariables.getValue(this)
        set(value) {
            myEnvironmentVariables.setValue(this, value)
        }

    var useMiseEnvironment: Boolean
        get() = myUseMiseEnvironment.getValue(this)
        set(value) {
            myUseMiseEnvironment.setValue(this, value)
        }
}

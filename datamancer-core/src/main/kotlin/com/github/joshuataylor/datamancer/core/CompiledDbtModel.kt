package com.github.joshuataylor.datamancer.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Result of compiling a dbt model via the Python bridge.
 * Used by the run configuration to execute compiled SQL.
 */
@Serializable
data class CompiledDbtModel(
    val success: Boolean,
    @SerialName("compiled_code") val compiledCode: String? = null,
    @SerialName("compiled_path") val compiledPath: String? = null,
    @SerialName("compiled_full_path") val compiledFullPath: String? = null,
    val error: String? = null,
)

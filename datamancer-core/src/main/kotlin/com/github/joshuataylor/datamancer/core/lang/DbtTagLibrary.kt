package com.github.joshuataylor.datamancer.core.lang

import com.intellij.jinja.tags.Jinja2TagLibrary

/**
 * Tag library for dbt-specific Jinja2 tags and functions.
 *
 * This class extends Jinja2TagLibrary to provide code completion and syntax support
 * for dbt's custom Jinja2 functions and macros.
 *
 * Tags are categorised into:
 * - **Parameterized tags**: Functions that accept arguments (e.g., ref('model'), source('schema', 'table'))
 * - **Unparameterized tags**: Variables and properties that don't take arguments (e.g., this, target, config)
 */
class DbtTagLibrary : Jinja2TagLibrary() {
    companion object {
        /**
         * dbt parameterized tags (functions that accept arguments).
         * These are dbt macros and functions that require parameters.
         */
        val DBT_PARAMETERIZED_TAGS: Set<String> = setOf(
            "debug",          // Debug logging
            "env_var",        // Environment variable access
            "fromjson",       // JSON parsing
            "fromyaml",       // YAML parsing
            "local_md5",      // MD5 hashing
            "log",            // Logging function
            "print",          // Print to console
            "ref",            // Reference to another model
            "run_query",      // Execute SQL query
            "set",            // Set variable
            "set_strict",     // Set variable with strict mode
            "source",         // Reference to source table
            "tojson",         // Convert to JSON
            "toyaml",         // Convert to YAML
            "var",            // Project variable access
            "zip"             // Zip function
        )

        /**
         * dbt unparameterized tags (properties and variables).
         * These are dbt context variables that are accessible without parameters.
         */
        val DBT_UNPARAMETERIZED_TAGS: Set<String> = setOf(
            "adapter",              // Database adapter
            "as_bool",              // Convert to boolean
            "as_native",            // Convert to native type
            "as_number",            // Convert to number
            "as_text",              // Convert to text
            "builtins",             // Python builtins
            "config",               // Model configuration
            "dbt",                  // dbt namespace
            "dbt_version",          // dbt version
            "dispatch",             // Macro dispatch
            "exceptions",           // Exception handling
            "execute",              // Execution context
            "flags",                // Command-line flags
            "graph",                // DAG graph
            "invocation_id",        // Unique invocation ID
            "model",                // Current model context
            "modules",              // Python modules
            "project_name",         // Project name
            "return",               // Return statement
            "run_started_at",       // Run start timestamp
            "schema",               // Schema name
            "schemas",              // Available schemas
            "selected_resources",   // Selected resources
            "target",               // Target configuration
            "this"                  // Current model reference
        )
    }

    /**
     * Returns the set of core (unparameterized) dbt tags.
     */
    override fun getCoreTags(): Set<String> {
        return DBT_UNPARAMETERIZED_TAGS
    }

    /**
     * Returns the set of parameterized dbt tags.
     */
    override fun getCoreParameterizedTags(): Set<String> {
        return DBT_PARAMETERIZED_TAGS
    }
}

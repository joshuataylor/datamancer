package com.github.joshuataylor.datamancer.core.inspection

import com.github.joshuataylor.datamancer.core.inspection.fix.AddPlusPrefixQuickFix
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import org.jetbrains.yaml.psi.YAMLDocument
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YamlPsiElementVisitor

/**
 * Reports config keys missing the '+' prefix in dbt_project.yml node-type blocks.
 *
 * In dbt_project.yml, hierarchical config values under node-type keys (models, seeds,
 * snapshots, tests, analyses) must be prefixed with '+' to distinguish them from
 * path/package groupings. Without the prefix, dbt v1.10+ raises MissingPlusPrefixDeprecation.
 */
class DbtProjectMissingPlusPrefixInspection : DbtDeprecationInspectionBase() {

    override fun getStaticDescription(): String = """
        Reports config keys missing the '+' prefix in dbt_project.yml.
        <p>
        Under node-type blocks (models, seeds, snapshots, tests, analyses),
        configuration values must use the '+' prefix to distinguish them from
        path groupings. For example, use <code>+materialized</code> instead of
        <code>materialized</code>.
        </p>
    """.trimIndent()

    override fun isApplicableFile(file: YAMLFile): Boolean = isDbtProjectYml(file)

    override fun buildYamlVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): YamlPsiElementVisitor {
        return object : YamlPsiElementVisitor() {

            override fun visitKeyValue(keyValue: YAMLKeyValue) {
                // Only interested in keys nested under a node-type block
                if (keyValue.keyText.startsWith("+")) return
                if (keyValue.keyText !in KNOWN_CONFIG_FIELDS) return

                // Walk up to find if we're inside a node-type block
                if (!isInsideNodeTypeBlock(keyValue)) return

                // Check there's no directory matching this key name
                if (directoryExistsForKey(keyValue)) return

                val keyElement = keyValue.key ?: return
                holder.registerProblem(
                    keyElement,
                    "Config key '${keyValue.keyText}' should be prefixed with '+'",
                    AddPlusPrefixQuickFix(keyValue.keyText),
                )
            }
        }
    }

    /**
     * Checks if the key-value is nested (at any depth) under one of the
     * node-type top-level keys (models, seeds, etc.) but is NOT a direct
     * child of the document root.
     */
    private fun isInsideNodeTypeBlock(keyValue: YAMLKeyValue): Boolean {
        var current = keyValue.parent
        var depth = 0
        while (current != null) {
            when (current) {
                is YAMLDocument -> return false
                is YAMLKeyValue -> {
                    if (current.keyText in NODE_TYPE_KEYS) {
                        // Must be at least one level deep inside the node-type block
                        return depth > 0
                    }
                    depth++
                }
            }
            current = current.parent
        }
        return false
    }

    /**
     * Heuristic: if a directory with the same name as the key exists at the
     * corresponding path level, this is likely a path grouping, not a config.
     */
    private fun directoryExistsForKey(keyValue: YAMLKeyValue): Boolean {
        val file = keyValue.containingFile?.virtualFile ?: return false
        val projectDir = file.parent ?: return false
        // Walk up to find the node-type key and collect path segments
        val segments = mutableListOf<String>()
        var current: Any? = keyValue.parent
        while (current != null) {
            if (current is YAMLKeyValue) {
                val key = current.keyText
                if (key in NODE_TYPE_KEYS) {
                    // The node-type key itself corresponds to a default path
                    // e.g. "models" -> models/ directory
                    segments.add(0, key)
                    break
                }
                segments.add(0, key)
            }
            current = (current as? PsiElement)?.parent
        }
        // Check if the directory path exists
        if (segments.isEmpty()) return false
        var dir = projectDir
        for (segment in segments) {
            dir = dir.findChild(segment) ?: return false
            if (!dir.isDirectory) return false
        }
        return true
    }

    companion object {
        private val NODE_TYPE_KEYS = setOf("models", "seeds", "snapshots", "tests", "analyses")

        /**
         * Config fields that require '+' prefix when used in dbt_project.yml node blocks.
         * Sourced from dbt-autofix fields_properties_configs.py and dbt documentation.
         */
        val KNOWN_CONFIG_FIELDS = setOf(
            // General
            "enabled", "tags", "pre_hook", "post_hook", "database", "schema", "alias",
            "persist_docs", "meta", "grants", "contract", "event_time",
            // Model-specific
            "materialized", "sql_header", "on_configuration_change", "unique_key",
            "incremental_strategy", "on_schema_change", "batch_size", "begin",
            "lookback", "concurrent_batches", "full_refresh",
            // Access/governance
            "docs", "group", "access",
            // Source freshness (moved from property)
            "freshness",
            // Snowflake
            "transient", "cluster_by", "automatic_clustering", "secure", "copy_grants",
            "snowflake_warehouse", "query_tag", "tmp_relation_type", "merge_update_columns",
            "target_lag", "table_format", "external_volume", "base_location_root",
            "base_location_subpath",
            // BigQuery
            "dataset", "project", "partition_by", "kms_key_name", "labels", "partitions",
            "grant_access_to", "hours_to_expiration", "require_partition_filter",
            "partition_expiration_days", "enable_refresh", "refresh_interval_minutes",
            "max_staleness", "enable_list_inference", "intermediate_format",
            "submission_method",
            // Postgres
            "unlogged", "indexes",
            // Redshift
            "sort_type", "dist", "sort", "bind", "backup", "auto_refresh",
            // Databricks
            "file_format", "location_root", "include_full_name_in_path",
            "clustered_by", "liquid_clustered_by", "auto_liquid_cluster", "buckets",
            "options", "merge_exclude_columns", "databricks_tags", "tblproperties",
            "zorder", "unique_tmp_table_suffix", "skip_non_matched_step",
            "skip_matched_step", "matched_condition", "not_matched_condition",
            "not_matched_by_source_action", "not_matched_by_source_condition",
            "target_alias", "source_alias", "merge_with_schema_evolution",
            // Seeds
            "quote_columns", "column_types", "delimiter",
            // Snapshots
            "strategy", "updated_at", "check_cols", "snapshot_meta_column_names",
            "hard_deletes", "dbt_valid_to_current",
            // Tests
            "fail_calc", "limit", "severity", "error_if", "warn_if",
            "store_failures", "where",
        )
    }
}

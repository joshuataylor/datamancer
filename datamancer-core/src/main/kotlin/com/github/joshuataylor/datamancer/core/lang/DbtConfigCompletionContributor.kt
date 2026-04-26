package com.github.joshuataylor.datamancer.core.lang

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.jinja.tags.Jinja2FunctionCall
import com.intellij.jinja.template.psi.impl.Jinja2VariableReferenceImpl
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext

/**
 * Provides code completion for dbt config() function keyword arguments.
 *
 * When the cursor is inside a `{{ config(...) }}` call, this contributor provides
 * completions for all valid dbt configuration options like `materialized`, `tags`,
 * `schema`, etc.
 *
 * Example:
 * ```sql
 * {{ config(
 *     mat<caret>  -- will suggest 'materialized'
 * ) }}
 * ```
 *
 * The config keys are based on the dbt JSON schema and documentation.
 */
class DbtConfigCompletionContributor : CompletionContributor() {

    init {
        // Register completion provider for any element inside Jinja2 templates
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(),
            DbtConfigCompletionProvider()
        )
    }

    companion object {
        /**
         * Config keys for dbt models with their descriptions and value types.
         * Based on dbt JSON schema and documentation.
         */
        val CONFIG_KEYS: List<ConfigKey> = listOf(
            // General configs (all resource types)
            ConfigKey("enabled", "Enable or disable the model", "boolean", "true"),
            ConfigKey("tags", "Tags for grouping and selecting models", "list/string", "['tag1', 'tag2']"),
            ConfigKey("meta", "Custom metadata dictionary", "dict", "{'key': 'value'}"),
            ConfigKey("group", "Assign model to a group for access control", "string", "'analytics'"),
            ConfigKey("docs", "Configure documentation generation", "dict", "{'show': True}"),

            // Model-specific configs
            ConfigKey(
                "materialized", "How the model is materialised in warehouse", "string",
                "'table'", listOf("table", "view", "incremental", "ephemeral", "materialized_view")
            ),
            ConfigKey("schema", "Custom schema for the model", "string", "'my_schema'"),
            ConfigKey("database", "Custom database for the model", "string", "'my_database'"),
            ConfigKey("alias", "Custom name for the model in warehouse", "string", "'my_alias'"),

            // Incremental configs
            ConfigKey("unique_key", "Column(s) for upsert/merge operations", "string/list", "'id'"),
            ConfigKey(
                "incremental_strategy", "Strategy for incremental builds", "string",
                "'merge'", listOf("append", "delete+insert", "merge", "insert_overwrite", "microbatch")
            ),
            ConfigKey(
                "on_schema_change", "Behaviour when schema changes", "string",
                "'ignore'", listOf("ignore", "fail", "append_new_columns", "sync_all_columns")
            ),
            ConfigKey("full_refresh", "Force full refresh on incremental models", "boolean", "False"),

            // Hooks
            ConfigKey("pre_hook", "SQL to run before model builds", "list/string", "[\"SQL\"]"),
            ConfigKey("post_hook", "SQL to run after model builds", "list/string", "[\"SQL\"]"),

            // Grants and permissions
            ConfigKey("grants", "Grant permissions on built objects", "dict", "{'select': ['user']}"),

            // Contract configs
            ConfigKey("contract", "Enable contract enforcement", "dict", "{'enforced': True}"),

            // Documentation
            ConfigKey(
                "persist_docs", "Persist column/relation docs to warehouse", "dict",
                "{'relation': True, 'columns': True}"
            ),

            // Partitioning and clustering
            ConfigKey("partition_by", "Column(s) for partitioning (BigQuery/Spark)", "dict/string", "'date_column'"),
            ConfigKey("cluster_by", "Column(s) for clustering (BigQuery/Snowflake)", "list/string", "['column']"),

            // BigQuery-specific
            ConfigKey("hours_to_expiration", "Table expiration in hours (BigQuery)", "number", "24"),
            ConfigKey("kms_key_name", "KMS key for encryption (BigQuery)", "string", "'projects/...'"),
            ConfigKey("labels", "Labels for the table (BigQuery)", "dict", "{'env': 'prod'}"),
            ConfigKey("grant_access_to", "Authorised views (BigQuery)", "list", "[{'project': '...'}]"),

            // Snowflake-specific
            ConfigKey("snowflake_warehouse", "Override warehouse for model (Snowflake)", "string", "'COMPUTE_WH'"),
            ConfigKey("copy_grants", "Copy grants from replaced table (Snowflake)", "boolean", "True"),
            ConfigKey("transient", "Create transient table (Snowflake)", "boolean", "False"),
            ConfigKey("query_tag", "Query tag for tracking (Snowflake)", "string", "'dbt'"),

            // Databricks/Spark-specific
            ConfigKey(
                "file_format", "File format for table (Databricks/Spark)", "string",
                "'delta'", listOf("delta", "parquet", "csv", "json", "text", "avro", "orc")
            ),
            ConfigKey("location_root", "Storage location root (Databricks)", "string", "'s3://bucket/'"),
            ConfigKey("tblproperties", "Table properties (Databricks/Spark)", "dict", "{'key': 'value'}"),

            // Redshift-specific
            ConfigKey("sort", "Sort key column(s) (Redshift)", "string/list", "'column'"),
            ConfigKey("dist", "Distribution style/key (Redshift)", "string", "'even'"),
            ConfigKey("bind", "Bind parameters late (Redshift)", "boolean", "False"),

            // Materialised view configs
            ConfigKey(
                "on_configuration_change", "Behaviour on config change for MV", "string",
                "'apply'", listOf("apply", "continue", "fail")
            ),
            ConfigKey("auto_refresh", "Enable auto-refresh for MV", "boolean", "True"),
            ConfigKey("backup", "Include in backup (Redshift MV)", "boolean", "True"),
            ConfigKey("target_lag", "Maximum staleness for MV (Snowflake)", "string", "'20 minutes'"),

            // SQL configs
            ConfigKey("sql_header", "SQL to prepend to compiled query", "string", "\"SET ...\""),

            // Event time (for incremental models)
            ConfigKey("event_time", "Column representing event timestamp", "string", "'created_at'"),

            // Access control
            ConfigKey(
                "access", "Model access level", "string",
                "'protected'", listOf("private", "protected", "public")
            )
        )

        /**
         * Quick lookup set for config key names.
         */
        val CONFIG_KEY_NAMES: Set<String> = CONFIG_KEYS.map { it.name }.toSet()
    }

    /**
     * Data class representing a config key with its metadata.
     */
    data class ConfigKey(
        val name: String,
        val description: String,
        val valueType: String,
        val exampleValue: String,
        val allowedValues: List<String>? = null
    )
}

/**
 * Completion provider that supplies config key suggestions inside config() calls.
 */
private class DbtConfigCompletionProvider : CompletionProvider<CompletionParameters>() {

    companion object {
        // Pattern to detect config( at start of Jinja expression
        private val CONFIG_CALL_PATTERN = Regex(
            """^\s*\{\{-?\s*config\s*\(""",
            RegexOption.IGNORE_CASE
        )
    }

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val position = parameters.position

        // Check if we're inside a config() function call
        val configContext = findConfigContext(position)
        if (configContext == null) {
            return
        }

        // Get existing config keys in this call to avoid duplicates
        val existingKeys = getExistingConfigKeys(configContext)

        // Add completions for all config keys not already present
        for (configKey in DbtConfigCompletionContributor.CONFIG_KEYS) {
            if (configKey.name in existingKeys) {
                continue
            }

            val lookupElement = LookupElementBuilder.create(configKey.name)
                .withTypeText(configKey.valueType)
                .withTailText(" = ${configKey.exampleValue}", true)
                .withPresentableText(configKey.name)
                .withInsertHandler { insertContext, _ ->
                    // Insert "=" after the key name
                    val document = insertContext.document
                    val tailOffset = insertContext.tailOffset
                    document.insertString(tailOffset, "=")
                    insertContext.editor.caretModel.moveToOffset(tailOffset + 1)
                }
                .withBoldness(isPrimaryConfig(configKey.name))

            result.addElement(
                PrioritizedLookupElement.withPriority(
                    lookupElement,
                    if (isPrimaryConfig(configKey.name)) 100.0 else 50.0
                )
            )
        }
    }

    /**
     * Finds the config() call context for the given element.
     * Works for both complete Jinja2FunctionCall elements and incomplete config() calls.
     *
     * @return The text of the config block if found, null otherwise
     */
    private fun findConfigContext(element: PsiElement): String? {
        // First try: Look for complete Jinja2FunctionCall
        val functionCall = PsiTreeUtil.getParentOfType(element, Jinja2FunctionCall::class.java)
        if (functionCall != null) {
            val callee = functionCall.callee
            val functionName = (callee as? Jinja2VariableReferenceImpl)?.name
            if (functionName == "config") {
                return functionCall.text
            }
        }

        // Second try: Walk up the tree looking for Jinja expression start
        // This handles incomplete config() calls that don't parse as Jinja2FunctionCall
        var current: PsiElement? = element
        while (current != null) {
            val text = current.text

            // Check if this element contains a config( pattern
            if (CONFIG_CALL_PATTERN.containsMatchIn(text)) {
                // Verify we're after the opening paren
                val configMatch = CONFIG_CALL_PATTERN.find(text)
                if (configMatch != null) {
                    val parenPos = text.indexOf('(', configMatch.range.first)
                    val elementOffset = element.textOffset - current.textOffset

                    // We're inside config() if we're positioned after the opening paren
                    if (parenPos >= 0 && elementOffset > parenPos) {
                        return text
                    }
                }
            }

            // Stop at file level or if we've gone too far up
            if (current.parent == null || current.parent == current.containingFile) {
                break
            }

            current = current.parent
        }

        return null
    }

    /**
     * Gets the config keys already present in the current config() call.
     */
    private fun getExistingConfigKeys(configText: String): Set<String> {
        val existingKeys = mutableSetOf<String>()

        // Match keyword argument patterns like "key="
        val keywordPattern = Regex("""(\w+)\s*=""")
        keywordPattern.findAll(configText).forEach { match ->
            existingKeys.add(match.groupValues[1])
        }

        return existingKeys
    }

    /**
     * Primary configs are shown in bold and prioritised higher.
     */
    private fun isPrimaryConfig(name: String): Boolean {
        return name in setOf(
            "materialized",
            "schema",
            "database",
            "alias",
            "tags",
            "enabled",
            "unique_key",
            "incremental_strategy"
        )
    }
}

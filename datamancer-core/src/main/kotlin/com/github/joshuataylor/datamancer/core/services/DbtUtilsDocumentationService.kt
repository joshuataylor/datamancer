package com.github.joshuataylor.datamancer.core.services

import com.github.joshuataylor.datamancer.core.services.DbtBuiltinDocumentationService.DbtBuiltinFunctionDoc
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap

/**
 * Service for loading and caching documentation for dbt_utils package functions.
 *
 * Documentation is loaded from bundled markdown files (generated from the dbt-utils
 * README) in `resources/docs/dbt-utils/`. Files are parsed on first access and
 * cached for the lifetime of the project.
 *
 * Reuses [DbtBuiltinFunctionDoc] and [DbtMarkdownConverter] from the built-in
 * documentation system since the markdown format is compatible.
 */
@Service(Service.Level.PROJECT)
class DbtUtilsDocumentationService(private val project: Project) {

    companion object {
        private val LOG = logger<DbtUtilsDocumentationService>()

        fun getInstance(project: Project): DbtUtilsDocumentationService = project.service()

        private const val RESOURCE_BASE_PATH = "/docs/dbt-utils/"
        private const val EXTERNAL_DOC_BASE_URL = "https://github.com/dbt-labs/dbt-utils#"

        /**
         * All known dbt_utils function names with documentation.
         */
        val KNOWN_FUNCTIONS: Set<String> = setOf(
            // Generic Tests
            "equal_rowcount",
            "fewer_rows_than",
            "equality",
            "expression_is_true",
            "recency",
            "at_least_one",
            "not_constant",
            "not_empty_string",
            "cardinality_equality",
            "not_null_proportion",
            "not_accepted_values",
            "relationships_where",
            "mutually_exclusive_ranges",
            "sequential_values",
            "unique_combination_of_columns",
            "accepted_range",
            // Introspective macros
            "get_column_values",
            "get_filtered_columns_in_relation",
            "get_relations_by_pattern",
            "get_relations_by_prefix",
            "get_query_results_as_dict",
            "get_single_value",
            // SQL generators
            "date_spine",
            "deduplicate",
            "haversine_distance",
            "group_by",
            "star",
            "union_relations",
            "generate_series",
            "generate_surrogate_key",
            "safe_add",
            "safe_divide",
            "safe_subtract",
            "pivot",
            "unpivot",
            "width_bucket",
            // Web macros
            "get_url_parameter",
            "get_url_host",
            "get_url_path",
            // Jinja helpers
            "pretty_time",
            "pretty_log_format",
            "log_info",
            "slugify",
        )

        /**
         * Known dbt package prefixes that have documentation available.
         */
        val KNOWN_PACKAGE_PREFIXES: Set<String> = setOf("dbt_utils")
    }

    /**
     * Cache of parsed documentation. Wrapped in [Optional] because [ConcurrentHashMap]
     * does not allow null values, and [getOrPut] delegates to `computeIfAbsent`.
     */
    private val cache = ConcurrentHashMap<String, Optional<DbtBuiltinFunctionDoc>>()

    /**
     * Returns parsed documentation for the given dbt_utils function name,
     * or null if no documentation is available.
     */
    fun getDocumentation(functionName: String): DbtBuiltinFunctionDoc? {
        return cache.getOrPut(functionName) {
            Optional.ofNullable(loadDocumentation(functionName))
        }.orElse(null)
    }

    /**
     * Returns the set of all function names that have documentation available.
     */
    fun getAvailableFunctions(): Set<String> = KNOWN_FUNCTIONS

    private fun loadDocumentation(functionName: String): DbtBuiltinFunctionDoc? {
        val filename = "$functionName.md"
        val resourcePath = "$RESOURCE_BASE_PATH$filename"

        val stream = javaClass.getResourceAsStream(resourcePath)
        if (stream == null) {
            LOG.debug("No dbt_utils documentation resource found for '$functionName' at $resourcePath")
            return null
        }

        val rawMarkdown = stream.bufferedReader().use { it.readText() }
        return parseMarkdown(functionName, rawMarkdown)
    }

    private fun parseMarkdown(functionName: String, rawMarkdown: String): DbtBuiltinFunctionDoc {
        val parsed = DbtMarkdownConverter.parse(rawMarkdown)
        val externalDocUrl = "${EXTERNAL_DOC_BASE_URL}${functionName}-source"

        return DbtBuiltinFunctionDoc(
            functionName = functionName,
            title = parsed.title,
            sidebarLabel = parsed.sidebarLabel,
            description = parsed.description,
            argsHtml = parsed.argsHtml,
            parameterNames = parsed.parameterNames,
            bodyHtml = parsed.bodyHtml,
            firstExample = parsed.firstExample,
            isParameterised = true,
            externalDocUrl = externalDocUrl
        )
    }
}

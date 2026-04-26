package com.github.joshuataylor.datamancer.core.services

import com.github.joshuataylor.datamancer.core.lang.DbtTagLibrary
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap

/**
 * Service for loading and caching documentation for dbt built-in Jinja2 functions.
 *
 * Documentation is loaded from bundled markdown files (copied from the dbt docs
 * repository) in `resources/docs/dbt-jinja-functions/`. Files are parsed on first
 * access and cached for the lifetime of the project.
 */
@Service(Service.Level.PROJECT)
class DbtBuiltinDocumentationService(private val project: Project) {

    companion object {
        private val LOG = logger<DbtBuiltinDocumentationService>()

        fun getInstance(project: Project): DbtBuiltinDocumentationService = project.service()

        /**
         * Maps dbt function names to their documentation markdown filenames.
         * Most names map directly (function name + ".md"); exceptions are listed explicitly.
         */
        private val FUNCTION_TO_FILENAME: Map<String, String> = buildMap {
            // Explicit overrides where the filename differs from the function name
            put("local_md5", "local-md5.md")
            put("debug", "debug-method.md")

            // All DbtTagLibrary functions that map directly
            val directNames = DbtTagLibrary.DBT_PARAMETERIZED_TAGS +
                DbtTagLibrary.DBT_UNPARAMETERIZED_TAGS -
                setOf("local_md5", "debug")
            for (name in directNames) {
                put(name, "$name.md")
            }

            // Additional functions not in DbtTagLibrary but with docs
            put("doc", "doc.md")
            put("thread_id", "thread_id.md")
        }

        private const val RESOURCE_BASE_PATH = "/docs/dbt-jinja-functions/"
        private const val EXTERNAL_DOC_BASE_URL = "https://docs.getdbt.com/reference/dbt-jinja-functions/"
    }

    /**
     * Parsed documentation for a single dbt built-in function.
     */
    data class DbtBuiltinFunctionDoc(
        val functionName: String,
        val title: String,
        val sidebarLabel: String,
        val description: String,
        val argsHtml: String?,
        val parameterNames: List<String>,
        val bodyHtml: String,
        val firstExample: String?,
        val isParameterised: Boolean,
        val externalDocUrl: String?
    )

    /**
     * Cache of parsed documentation. Wrapped in [Optional] because [ConcurrentHashMap]
     * does not allow null values, and [getOrPut] delegates to `computeIfAbsent`.
     */
    private val cache = ConcurrentHashMap<String, Optional<DbtBuiltinFunctionDoc>>()

    /**
     * Returns parsed documentation for the given function name, or null if no
     * documentation is available.
     */
    fun getDocumentation(functionName: String): DbtBuiltinFunctionDoc? {
        return cache.getOrPut(functionName) {
            Optional.ofNullable(loadDocumentation(functionName))
        }.orElse(null)
    }

    /**
     * Returns the set of all function names that have documentation available.
     */
    fun getAvailableFunctions(): Set<String> = FUNCTION_TO_FILENAME.keys

    private fun loadDocumentation(functionName: String): DbtBuiltinFunctionDoc? {
        val filename = FUNCTION_TO_FILENAME[functionName] ?: "$functionName.md"
        val resourcePath = "$RESOURCE_BASE_PATH$filename"

        val stream = javaClass.getResourceAsStream(resourcePath)
        if (stream == null) {
            LOG.debug("No documentation resource found for '$functionName' at $resourcePath")
            return null
        }

        val rawMarkdown = stream.bufferedReader().use { it.readText() }
        return parseMarkdown(functionName, rawMarkdown)
    }

    private fun parseMarkdown(functionName: String, rawMarkdown: String): DbtBuiltinFunctionDoc {
        val parsed = DbtMarkdownConverter.parse(rawMarkdown)
        val isParameterised = functionName in DbtTagLibrary.DBT_PARAMETERIZED_TAGS
        val externalDocUrl = if (parsed.id.isNotBlank()) {
            "$EXTERNAL_DOC_BASE_URL${parsed.id}"
        } else {
            null
        }

        return DbtBuiltinFunctionDoc(
            functionName = functionName,
            title = parsed.title,
            sidebarLabel = parsed.sidebarLabel,
            description = parsed.description,
            argsHtml = parsed.argsHtml,
            parameterNames = parsed.parameterNames,
            bodyHtml = parsed.bodyHtml,
            firstExample = parsed.firstExample,
            isParameterised = isParameterised,
            externalDocUrl = externalDocUrl
        )
    }
}

package com.github.joshuataylor.datamancer.core.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager

/**
 * Service for indexing and finding dbt generic test definitions from .sql files
 * in tests/generic/ and macros/ directories.
 *
 * Generic tests are defined in .sql files using Jinja2 block tag syntax:
 * ```
 * {percent} test is_positive(model, column_name) {percent}
 *   SELECT * FROM {{ model }} WHERE {{ column_name }} < 0
 * {percent} endtest {percent}
 * ```
 *
 * Test references in YAML schema files like `data_tests: [is_positive]` can then
 * be resolved back to their definitions for go-to-definition.
 */
@Service(Service.Level.PROJECT)
class DbtGenericTestIndexService(private val project: Project) {

    companion object {
        fun getInstance(project: Project): DbtGenericTestIndexService = project.service()

        private const val DEFAULT_TEST_DIR = "tests"
        private const val GENERIC_SUBDIR = "generic"
        private const val DEFAULT_MACRO_DIR = "macros"

        /**
         * Regex to match generic test definition tags in .sql files.
         *
         * Matches patterns like:
         * - `{percent} test name(model, column_name) {percent}`
         * - `{percent}- test name(model, column_name) -{percent}`
         * - `{percent} test name(model, column_name, min_value=0) {percent}`
         *
         * Group 1 captures the test name.
         */
        val TEST_DEFINITION_REGEX = Regex(
            """\{%-?\s*test\s+(\w+)\s*\(""",
            RegexOption.MULTILINE
        )

        /**
         * Regex to extract the parameter list from a test definition.
         * Applied to the text starting from the opening parenthesis.
         */
        private val PARAMS_REGEX = Regex("""\(([^)]*)\)""")
    }

    /**
     * Represents a dbt generic test definition found in a .sql file.
     *
     * @param name The test name (e.g. "is_positive")
     * @param file The .sql file containing the definition
     * @param textOffset Character offset of the test name within the file text
     * @param nameLength Length of the test name string
     * @param parameters Parameter names extracted from the definition
     */
    data class TestDefinition(
        val name: String,
        val file: VirtualFile,
        val textOffset: Int,
        val nameLength: Int,
        val parameters: List<String> = emptyList()
    )

    /**
     * Finds all generic test definitions across all dbt projects in the workspace.
     *
     * Scans tests/generic/ and macros/ directories for .sql files and parses
     * them for `{percent} test name(...) {percent}` definitions.
     *
     * @return Map of test name to TestDefinition
     */
    fun getAllGenericTests(): Map<String, TestDefinition> {
        val tests = mutableMapOf<String, TestDefinition>()
        val indexService = DatamancerDbtProjectIndexService.getInstance(project)
        val allConfigs = indexService.getAllDbtConfigsSync()

        if (allConfigs.isEmpty()) {
            return tests
        }

        val vfm = VirtualFileManager.getInstance()

        for ((_, config) in allConfigs) {
            // Scan tests/generic/ directory
            val genericTestsPath = "${config.projectRoot}/$DEFAULT_TEST_DIR/$GENERIC_SUBDIR"
            val genericTestsDir = vfm.findFileByUrl("file://$genericTestsPath")
            if (genericTestsDir != null) {
                val sqlFiles = mutableListOf<VirtualFile>()
                collectSqlFiles(genericTestsDir, sqlFiles)
                for (sqlFile in sqlFiles) {
                    val fileDefs = parseTestsFromFile(sqlFile)
                    for (testDef in fileDefs) {
                        tests[testDef.name] = testDef
                    }
                }
            }

            // Also scan macros/ directory (tests can be defined alongside macros)
            val macrosPath = "${config.projectRoot}/$DEFAULT_MACRO_DIR"
            val macrosDir = vfm.findFileByUrl("file://$macrosPath")
            if (macrosDir != null) {
                val sqlFiles = mutableListOf<VirtualFile>()
                collectSqlFiles(macrosDir, sqlFiles)
                for (sqlFile in sqlFiles) {
                    val fileDefs = parseTestsFromFile(sqlFile)
                    for (testDef in fileDefs) {
                        tests[testDef.name] = testDef
                    }
                }
            }
        }

        return tests
    }

    /**
     * Finds a specific generic test definition by name.
     *
     * @param testName The name of the test to find
     * @return The TestDefinition, or null if not found
     */
    fun findGenericTest(testName: String): TestDefinition? {
        return getAllGenericTests()[testName]
    }

    /**
     * Gets all available generic test names for code completion.
     */
    fun getAllGenericTestNames(): List<String> {
        return getAllGenericTests().keys.toList()
    }

    /**
     * Parses generic test definitions from a single .sql file.
     *
     * @param file The .sql file to parse
     * @return List of TestDefinition instances found in the file
     */
    private fun parseTestsFromFile(file: VirtualFile): List<TestDefinition> {
        val tests = mutableListOf<TestDefinition>()

        val content = try {
            String(file.contentsToByteArray(), file.charset)
        } catch (_: Exception) {
            return tests
        }

        val matches = TEST_DEFINITION_REGEX.findAll(content)

        for (match in matches) {
            val testName = match.groupValues[1]
            val nameStart = match.range.first + match.value.indexOf(testName)

            // Extract parameters from the text following the test name
            val afterName = content.substring(match.range.first)
            val params = extractParameters(afterName)

            tests.add(
                TestDefinition(
                    name = testName,
                    file = file,
                    textOffset = nameStart,
                    nameLength = testName.length,
                    parameters = params
                )
            )
        }

        return tests
    }

    /**
     * Extracts parameter names from a test definition tag text.
     *
     * Given text starting from `{percent} test name(model, column_name) {percent}`,
     * returns `["model", "column_name"]`.
     */
    private fun extractParameters(tagText: String): List<String> {
        val paramsMatch = PARAMS_REGEX.find(tagText) ?: return emptyList()
        val paramsText = paramsMatch.groupValues[1].trim()

        if (paramsText.isEmpty()) {
            return emptyList()
        }

        return paramsText.split(",").map { param ->
            // Take only the name part (before any '=' default value)
            param.trim().split("=")[0].trim()
        }.filter { it.isNotEmpty() }
    }

    /**
     * Recursively collects all .sql files from a directory.
     */
    private fun collectSqlFiles(directory: VirtualFile, collector: MutableList<VirtualFile>) {
        if (!directory.isDirectory) {
            return
        }

        for (child in directory.children) {
            if (child.isDirectory) {
                collectSqlFiles(child, collector)
            } else if (child.extension == "sql") {
                collector.add(child)
            }
        }
    }
}

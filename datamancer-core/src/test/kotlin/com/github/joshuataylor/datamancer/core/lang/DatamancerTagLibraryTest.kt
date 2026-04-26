package com.github.joshuataylor.datamancer.core.lang

import com.intellij.jinja.tags.Jinja2TagLibrary
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for DbtTagLibrary.
 * Tests dbt-specific Jinja2 tag registration and availability.
 */
class DbtTagLibraryTest : BasePlatformTestCase() {
    private lateinit var tagLibrary: DbtTagLibrary

    override fun setUp() {
        super.setUp()
        tagLibrary = DbtTagLibrary()
    }

    // Basic instantiation and type tests
    fun testTagLibraryCanBeInstantiated() {
        assertNotNull(tagLibrary)
    }

    fun testTagLibraryIsJinja2TagLibrary() {
        assertTrue(tagLibrary is Jinja2TagLibrary)
    }

    // Parameterized tags tests
    fun testParameterizedTagsAreNotEmpty() {
        val tags = tagLibrary.coreParameterizedTags
        assertNotNull(tags)
        assertTrue(tags.isNotEmpty())
    }

    fun testParameterizedTagsContainRef() {
        val tags = tagLibrary.coreParameterizedTags
        assertTrue("ref should be in parameterized tags", tags.contains("ref"))
    }

    fun testParameterizedTagsContainSource() {
        val tags = tagLibrary.coreParameterizedTags
        assertTrue("source should be in parameterized tags", tags.contains("source"))
    }

    fun testParameterizedTagsContainVar() {
        val tags = tagLibrary.coreParameterizedTags
        assertTrue("var should be in parameterized tags", tags.contains("var"))
    }

    fun testParameterizedTagsContainLog() {
        val tags = tagLibrary.coreParameterizedTags
        assertTrue("log should be in parameterized tags", tags.contains("log"))
    }

    fun testParameterizedTagsContainRunQuery() {
        val tags = tagLibrary.coreParameterizedTags
        assertTrue("run_query should be in parameterized tags", tags.contains("run_query"))
    }

    fun testParameterizedTagsContainEnvVar() {
        val tags = tagLibrary.coreParameterizedTags
        assertTrue("env_var should be in parameterized tags", tags.contains("env_var"))
    }

    fun testParameterizedTagsContainFromJson() {
        val tags = tagLibrary.coreParameterizedTags
        assertTrue("fromjson should be in parameterized tags", tags.contains("fromjson"))
    }

    fun testParameterizedTagsContainToJson() {
        val tags = tagLibrary.coreParameterizedTags
        assertTrue("tojson should be in parameterized tags", tags.contains("tojson"))
    }

    fun testParameterizedTagsCount() {
        val tags = tagLibrary.coreParameterizedTags
        // Should have 16 parameterized tags based on decompiled plugin
        assertEquals(16, tags.size)
    }

    // Unparameterized tags tests
    fun testUnparameterizedTagsAreNotEmpty() {
        val tags = tagLibrary.coreTags
        assertNotNull(tags)
        assertTrue(tags.isNotEmpty())
    }

    fun testUnparameterizedTagsContainConfig() {
        val tags = tagLibrary.coreTags
        assertTrue("config should be in unparameterized tags", tags.contains("config"))
    }

    fun testUnparameterizedTagsContainThis() {
        val tags = tagLibrary.coreTags
        assertTrue("this should be in unparameterized tags", tags.contains("this"))
    }

    fun testUnparameterizedTagsContainTarget() {
        val tags = tagLibrary.coreTags
        assertTrue("target should be in unparameterized tags", tags.contains("target"))
    }

    fun testUnparameterizedTagsContainAdapter() {
        val tags = tagLibrary.coreTags
        assertTrue("adapter should be in unparameterized tags", tags.contains("adapter"))
    }

    fun testUnparameterizedTagsContainModel() {
        val tags = tagLibrary.coreTags
        assertTrue("model should be in unparameterized tags", tags.contains("model"))
    }

    fun testUnparameterizedTagsContainDbt() {
        val tags = tagLibrary.coreTags
        assertTrue("dbt should be in unparameterized tags", tags.contains("dbt"))
    }

    fun testUnparameterizedTagsContainExecute() {
        val tags = tagLibrary.coreTags
        assertTrue("execute should be in unparameterized tags", tags.contains("execute"))
    }

    fun testUnparameterizedTagsContainGraph() {
        val tags = tagLibrary.coreTags
        assertTrue("graph should be in unparameterized tags", tags.contains("graph"))
    }

    fun testUnparameterizedTagsCount() {
        val tags = tagLibrary.coreTags
        // Should have 25 unparameterized tags based on decompiled plugin
        assertEquals(25, tags.size)
    }

    // Companion object tests
    fun testCompanionParameterizedTagsAreAccessible() {
        val tags = DbtTagLibrary.DBT_PARAMETERIZED_TAGS
        assertNotNull(tags)
        assertTrue(tags.isNotEmpty())
    }

    fun testCompanionUnparameterizedTagsAreAccessible() {
        val tags = DbtTagLibrary.DBT_UNPARAMETERIZED_TAGS
        assertNotNull(tags)
        assertTrue(tags.isNotEmpty())
    }

    fun testCompanionTagsMatchInstanceTags() {
        assertEquals(
            DbtTagLibrary.DBT_PARAMETERIZED_TAGS,
            tagLibrary.coreParameterizedTags
        )
        assertEquals(
            DbtTagLibrary.DBT_UNPARAMETERIZED_TAGS,
            tagLibrary.coreTags
        )
    }

    // Tag categorisation tests
    fun testNoOverlapBetweenParameterizedAndUnparameterized() {
        val parameterized = tagLibrary.coreParameterizedTags
        val unparameterized = tagLibrary.coreTags

        val overlap = parameterized.intersect(unparameterized)
        assertTrue(
            "No tags should appear in both sets, but found: $overlap",
            overlap.isEmpty()
        )
    }

    fun testTagsAreImmutable() {
        val tags = tagLibrary.coreParameterizedTags
        // Attempting to modify should not affect the library
        val mutableCopy = tags.toMutableSet()
        mutableCopy.add("new_tag")

        // Original should still be unchanged
        assertFalse(tagLibrary.coreParameterizedTags.contains("new_tag"))
    }

    // Specific dbt functionality tests
    fun testCommonDbtFunctionsArePresent() {
        val parameterized = tagLibrary.coreParameterizedTags

        val commonFunctions = listOf("ref", "source", "var", "log", "run_query")
        commonFunctions.forEach { function ->
            assertTrue(
                "$function is a common dbt function and should be present",
                parameterized.contains(function)
            )
        }
    }

    fun testCommonDbtContextVariablesArePresent() {
        val unparameterized = tagLibrary.coreTags

        val commonVariables = listOf("this", "target", "config", "adapter", "execute")
        commonVariables.forEach { variable ->
            assertTrue(
                "$variable is a common dbt context variable and should be present",
                unparameterized.contains(variable)
            )
        }
    }

    // Multiple instance tests
    fun testMultipleInstancesReturnSameTags() {
        val library1 = DbtTagLibrary()
        val library2 = DbtTagLibrary()

        assertEquals(library1.coreParameterizedTags, library2.coreParameterizedTags)
        assertEquals(library1.coreTags, library2.coreTags)
    }

    // Edge case tests
    fun testAllTagsAreLowercase() {
        val allTags = tagLibrary.coreParameterizedTags + tagLibrary.coreTags

        allTags.forEach { tag ->
            assertEquals(
                "All tags should be lowercase: $tag",
                tag.lowercase(),
                tag
            )
        }
    }

    fun testNoTagsContainSpaces() {
        val allTags = tagLibrary.coreParameterizedTags + tagLibrary.coreTags

        allTags.forEach { tag ->
            assertFalse(
                "Tags should not contain spaces: $tag",
                tag.contains(" ")
            )
        }
    }

    fun testAllTagsAreValidIdentifiers() {
        val allTags = tagLibrary.coreParameterizedTags + tagLibrary.coreTags

        allTags.forEach { tag ->
            assertTrue(
                "Tag should be a valid identifier: $tag",
                tag.matches(Regex("[a-z_][a-z0-9_]*"))
            )
        }
    }
}

package com.github.joshuataylor.datamancer.core.services

import com.github.joshuataylor.datamancer.core.lang.DbtTagLibrary
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for [DbtBuiltinDocumentationService].
 * Tests resource loading, frontmatter parsing, filename mapping, and caching.
 */
class DbtBuiltinDocumentationServiceTest : BasePlatformTestCase() {

    private fun getService(): DbtBuiltinDocumentationService {
        return DbtBuiltinDocumentationService.getInstance(project)
    }

    // -- Resource loading for known functions --

    fun testGetDocumentationForRef() {
        val doc = getService().getDocumentation("ref")
        assertNotNull("Should find documentation for ref", doc)
        assertEquals("ref", doc!!.functionName)
        assertTrue("ref should be parameterised", doc.isParameterised)
        assertTrue(doc.description.isNotBlank())
        assertTrue(doc.sidebarLabel.isNotBlank())
    }

    fun testGetDocumentationForSource() {
        val doc = getService().getDocumentation("source")
        assertNotNull("Should find documentation for source", doc)
        assertEquals("source", doc!!.functionName)
        assertTrue("source should be parameterised", doc.isParameterised)
    }

    fun testGetDocumentationForVar() {
        val doc = getService().getDocumentation("var")
        assertNotNull("Should find documentation for var", doc)
        assertTrue(doc!!.isParameterised)
    }

    fun testGetDocumentationForThis() {
        val doc = getService().getDocumentation("this")
        assertNotNull("Should find documentation for this", doc)
        assertFalse("this should be unparameterised", doc!!.isParameterised)
    }

    fun testGetDocumentationForTarget() {
        val doc = getService().getDocumentation("target")
        assertNotNull("Should find documentation for target", doc)
        assertFalse(doc!!.isParameterised)
    }

    fun testGetDocumentationForConfig() {
        val doc = getService().getDocumentation("config")
        assertNotNull("Should find documentation for config", doc)
    }

    fun testGetDocumentationForEnvVar() {
        val doc = getService().getDocumentation("env_var")
        assertNotNull("Should find documentation for env_var", doc)
        assertTrue(doc!!.isParameterised)
    }

    fun testGetDocumentationForRunQuery() {
        val doc = getService().getDocumentation("run_query")
        assertNotNull("Should find documentation for run_query", doc)
        assertTrue(doc!!.isParameterised)
    }

    // -- Filename mapping overrides --

    fun testGetDocumentationForLocalMd5() {
        val doc = getService().getDocumentation("local_md5")
        assertNotNull("Should find documentation for local_md5 (mapped to local-md5.md)", doc)
    }

    fun testGetDocumentationForDebug() {
        val doc = getService().getDocumentation("debug")
        assertNotNull("Should find documentation for debug (mapped to debug-method.md)", doc)
    }

    // -- Functions without doc files --

    fun testGetDocumentationForAsTextReturnsNull() {
        val doc = getService().getDocumentation("as_text")
        assertNull("as_text has no doc file, should return null", doc)
    }

    fun testGetDocumentationForSetStrictReturnsNull() {
        val doc = getService().getDocumentation("set_strict")
        assertNull("set_strict has no doc file, should return null", doc)
    }

    fun testGetDocumentationForDbtReturnsNull() {
        val doc = getService().getDocumentation("dbt")
        assertNull("dbt has no doc file, should return null", doc)
    }

    fun testGetDocumentationForUnknownFunctionReturnsNull() {
        val doc = getService().getDocumentation("totally_unknown_function_xyz")
        assertNull(doc)
    }

    // -- Frontmatter extraction --

    fun testTitleIsExtracted() {
        val doc = getService().getDocumentation("ref")
        assertNotNull(doc)
        assertTrue("Title should be non-empty", doc!!.title.isNotBlank())
    }

    fun testSidebarLabelIsExtracted() {
        val doc = getService().getDocumentation("ref")
        assertNotNull(doc)
        assertEquals("ref", doc!!.sidebarLabel)
    }

    fun testDescriptionIsExtracted() {
        val doc = getService().getDocumentation("ref")
        assertNotNull(doc)
        assertTrue("Description should be non-empty", doc!!.description.isNotBlank())
    }

    // -- Content extraction --

    fun testRefHasFirstExample() {
        val doc = getService().getDocumentation("ref")
        assertNotNull(doc)
        assertNotNull("ref should have a first example", doc!!.firstExample)
    }

    fun testSourceHasArgs() {
        val doc = getService().getDocumentation("source")
        assertNotNull(doc)
        assertNotNull("source should have args", doc!!.argsHtml)
        assertTrue("source should have parameter names", doc.parameterNames.isNotEmpty())
    }

    fun testRunQueryHasArgs() {
        val doc = getService().getDocumentation("run_query")
        assertNotNull(doc)
        assertNotNull("run_query should have args", doc!!.argsHtml)
        assertTrue(doc.parameterNames.contains("sql"))
    }

    fun testBodyHtmlIsNonEmpty() {
        val doc = getService().getDocumentation("ref")
        assertNotNull(doc)
        assertTrue("bodyHtml should be non-empty", doc!!.bodyHtml.isNotBlank())
    }

    // -- External documentation URL --

    fun testRefHasExternalDocUrl() {
        val doc = getService().getDocumentation("ref")
        assertNotNull(doc)
        assertEquals(
            "https://docs.getdbt.com/reference/dbt-jinja-functions/ref",
            doc!!.externalDocUrl
        )
    }

    fun testSourceHasExternalDocUrl() {
        val doc = getService().getDocumentation("source")
        assertNotNull(doc)
        assertEquals(
            "https://docs.getdbt.com/reference/dbt-jinja-functions/source",
            doc!!.externalDocUrl
        )
    }

    fun testAdapterHasExternalDocUrl() {
        val doc = getService().getDocumentation("adapter")
        assertNotNull(doc)
        assertEquals(
            "https://docs.getdbt.com/reference/dbt-jinja-functions/adapter",
            doc!!.externalDocUrl
        )
    }

    // -- Caching --

    fun testCachingReturnsSameInstance() {
        val service = getService()
        val doc1 = service.getDocumentation("ref")
        val doc2 = service.getDocumentation("ref")
        assertSame("Cached result should be the same instance", doc1, doc2)
    }

    // -- Coverage for all DbtTagLibrary entries --

    fun testAllParameterisedTagsHaveMappingEntry() {
        val service = getService()
        val available = service.getAvailableFunctions()
        for (tag in DbtTagLibrary.DBT_PARAMETERIZED_TAGS) {
            assertTrue(
                "Parameterised tag '$tag' should have a mapping entry",
                tag in available
            )
        }
    }

    fun testAllUnparameterisedTagsHaveMappingEntry() {
        val service = getService()
        val available = service.getAvailableFunctions()
        for (tag in DbtTagLibrary.DBT_UNPARAMETERIZED_TAGS) {
            assertTrue(
                "Unparameterised tag '$tag' should have a mapping entry",
                tag in available
            )
        }
    }
}

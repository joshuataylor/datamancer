package com.github.joshuataylor.datamancer.core.services

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for [DbtUtilsDocumentationService].
 * Tests resource loading, frontmatter parsing, caching, and known function coverage.
 */
class DbtUtilsDocumentationServiceTest : BasePlatformTestCase() {

    private fun getService(): DbtUtilsDocumentationService {
        return DbtUtilsDocumentationService.getInstance(project)
    }

    // -- Resource loading for known functions --

    fun testGetDocumentationForGenerateSurrogateKey() {
        val doc = getService().getDocumentation("generate_surrogate_key")
        assertNotNull("Should find documentation for generate_surrogate_key", doc)
        assertEquals("generate_surrogate_key", doc!!.functionName)
        assertTrue("generate_surrogate_key should be parameterised", doc.isParameterised)
        assertTrue(doc.description.isNotBlank())
    }

    fun testGetDocumentationForStar() {
        val doc = getService().getDocumentation("star")
        assertNotNull("Should find documentation for star", doc)
        assertEquals("star", doc!!.functionName)
    }

    fun testGetDocumentationForPivot() {
        val doc = getService().getDocumentation("pivot")
        assertNotNull("Should find documentation for pivot", doc)
        assertEquals("pivot", doc!!.functionName)
    }

    fun testGetDocumentationForEqualRowcount() {
        val doc = getService().getDocumentation("equal_rowcount")
        assertNotNull("Should find documentation for equal_rowcount", doc)
        assertEquals("equal_rowcount", doc!!.functionName)
    }

    fun testGetDocumentationForGetColumnValues() {
        val doc = getService().getDocumentation("get_column_values")
        assertNotNull("Should find documentation for get_column_values", doc)
    }

    fun testGetDocumentationForDeduplicate() {
        val doc = getService().getDocumentation("deduplicate")
        assertNotNull("Should find documentation for deduplicate", doc)
    }

    fun testGetDocumentationForGetUrlParameter() {
        val doc = getService().getDocumentation("get_url_parameter")
        assertNotNull("Should find documentation for get_url_parameter", doc)
    }

    fun testGetDocumentationForSlugify() {
        val doc = getService().getDocumentation("slugify")
        assertNotNull("Should find documentation for slugify", doc)
    }

    // -- Functions without doc files --

    fun testGetDocumentationForUnknownFunctionReturnsNull() {
        val doc = getService().getDocumentation("totally_unknown_function_xyz")
        assertNull(doc)
    }

    fun testGetDocumentationForEmptyNameReturnsNull() {
        val doc = getService().getDocumentation("")
        assertNull(doc)
    }

    // -- Frontmatter extraction --

    fun testTitleIsExtracted() {
        val doc = getService().getDocumentation("generate_surrogate_key")
        assertNotNull(doc)
        assertTrue("Title should be non-empty", doc!!.title.isNotBlank())
    }

    fun testSidebarLabelIsExtracted() {
        val doc = getService().getDocumentation("generate_surrogate_key")
        assertNotNull(doc)
        assertEquals("generate_surrogate_key", doc!!.sidebarLabel)
    }

    fun testDescriptionIsExtracted() {
        val doc = getService().getDocumentation("generate_surrogate_key")
        assertNotNull(doc)
        assertTrue("Description should be non-empty", doc!!.description.isNotBlank())
    }

    // -- Content extraction --

    fun testBodyHtmlIsNonEmpty() {
        val doc = getService().getDocumentation("generate_surrogate_key")
        assertNotNull(doc)
        assertTrue("bodyHtml should be non-empty", doc!!.bodyHtml.isNotBlank())
    }

    fun testStarHasArgs() {
        val doc = getService().getDocumentation("star")
        assertNotNull(doc)
        assertNotNull("star should have args", doc!!.argsHtml)
        assertTrue("star should have parameter names", doc.parameterNames.isNotEmpty())
    }

    fun testDeduplicateHasArgs() {
        val doc = getService().getDocumentation("deduplicate")
        assertNotNull(doc)
        assertNotNull("deduplicate should have args", doc!!.argsHtml)
        assertTrue(doc.parameterNames.contains("relation"))
    }

    fun testHaversineDistanceHasArgs() {
        val doc = getService().getDocumentation("haversine_distance")
        assertNotNull(doc)
        assertNotNull("haversine_distance should have args", doc!!.argsHtml)
        assertTrue(doc.parameterNames.contains("lat1"))
    }

    // -- External documentation URL --

    fun testExternalDocUrl() {
        val doc = getService().getDocumentation("generate_surrogate_key")
        assertNotNull(doc)
        assertEquals(
            "https://github.com/dbt-labs/dbt-utils#generate_surrogate_key-source",
            doc!!.externalDocUrl
        )
    }

    fun testStarExternalDocUrl() {
        val doc = getService().getDocumentation("star")
        assertNotNull(doc)
        assertEquals(
            "https://github.com/dbt-labs/dbt-utils#star-source",
            doc!!.externalDocUrl
        )
    }

    // -- All functions are parameterised --

    fun testAllFunctionsAreParameterised() {
        val service = getService()
        for (functionName in DbtUtilsDocumentationService.KNOWN_FUNCTIONS) {
            val doc = service.getDocumentation(functionName)
            if (doc != null) {
                assertTrue(
                    "dbt_utils function '$functionName' should be parameterised",
                    doc.isParameterised
                )
            }
        }
    }

    // -- Caching --

    fun testCachingReturnsSameInstance() {
        val service = getService()
        val doc1 = service.getDocumentation("generate_surrogate_key")
        val doc2 = service.getDocumentation("generate_surrogate_key")
        assertSame("Cached result should be the same instance", doc1, doc2)
    }

    // -- Coverage for known functions --

    fun testAllKnownFunctionsHaveDocumentation() {
        val service = getService()
        for (functionName in DbtUtilsDocumentationService.KNOWN_FUNCTIONS) {
            val doc = service.getDocumentation(functionName)
            assertNotNull(
                "Known function '$functionName' should have documentation",
                doc
            )
        }
    }
}

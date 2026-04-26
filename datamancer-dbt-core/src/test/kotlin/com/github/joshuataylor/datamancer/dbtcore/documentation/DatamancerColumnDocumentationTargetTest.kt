package com.github.joshuataylor.datamancer.dbtcore.documentation

import com.intellij.platform.backend.documentation.DocumentationData
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.pom.Navigatable
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.yaml.psi.YAMLFile

/**
 * Tests for DatamancerColumnDocumentationTarget.
 * Tests HTML output, presentation, navigation, and pointer behaviour.
 */
class DatamancerColumnDocumentationTargetTest : BasePlatformTestCase() {

    // -- Interface conformance --

    fun testTargetImplementsDocumentationTarget() {
        val target = createTarget()
        assertTrue(target is DocumentationTarget)
    }

    // -- computePresentation --

    fun testComputePresentationContainsColumnName() {
        val target = createTarget(columnName = "customer_id", columnDataType = null)
        val presentation = target.computePresentation()
        assertNotNull(presentation)
        assertEquals("customer_id", presentation.presentableText)
    }

    fun testComputePresentationContainsDataType() {
        val target = createTarget(columnName = "customer_id", columnDataType = "text")
        val presentation = target.computePresentation()
        assertNotNull(presentation)
        assertEquals("customer_id: text", presentation.presentableText)
    }

    // -- computeDocumentation: definition section --

    fun testDocumentationContainsModelName() {
        val target = createTarget(modelName = "stg_customers")
        val html = getDocumentationHtml(target)
        assertNotNull(html)
        assertTrue("Should contain model name", html!!.contains("stg_customers"))
    }

    fun testDocumentationContainsColumnName() {
        val target = createTarget(columnName = "customer_id")
        val html = getDocumentationHtml(target)
        assertNotNull(html)
        assertTrue("Should contain column name", html!!.contains("customer_id"))
    }

    fun testDocumentationContainsSchemaFilePath() {
        val target = createTarget(schemaFilePath = "dbt/models/schema.yml")
        val html = getDocumentationHtml(target)
        assertNotNull(html)
        assertTrue("Should contain schema file path", html!!.contains("dbt/models/schema.yml"))
    }

    fun testDocumentationContainsDataType() {
        val target = createTarget(columnName = "customer_id", columnDataType = "text")
        val html = getDocumentationHtml(target)
        assertNotNull(html)
        assertTrue("Should contain data type", html!!.contains("text"))
    }

    fun testDocumentationOmitsDataTypeWhenNull() {
        val target = createTarget(columnName = "customer_id", columnDataType = null)
        val html = getDocumentationHtml(target)
        assertNotNull(html)
        // Should not contain the ": " separator that appears before a data type
        val definitionSection = html!!.substringBefore("class=\"content\"")
        assertFalse(
            "Should not contain CLASS_NAME styled span when no data type",
            definitionSection.contains("CLASS_NAME")
        )
    }

    // -- computeDocumentation: content section --

    fun testDocumentationContainsDescription() {
        val target = createTarget(columnDescription = "Unique customer identifier")
        val html = getDocumentationHtml(target)
        assertNotNull(html)
        assertTrue("Should contain description text", html!!.contains("Unique customer identifier"))
    }

    fun testDocumentationDescriptionIsNotGrayed() {
        val target = createTarget(columnDescription = "Unique customer identifier")
        val html = getDocumentationHtml(target)
        assertNotNull(html)
        // The description text should not be wrapped in a grayed element.
        // A grayed description would look like: grayed">...Unique customer identifier
        assertFalse(
            "Description should not be in grayed styling",
            html!!.contains("grayed\">Unique customer identifier")
        )
    }

    fun testDocumentationDescriptionHasNoSqlCommentPrefix() {
        val target = createTarget(columnDescription = "Unique customer identifier")
        val html = getDocumentationHtml(target)
        assertNotNull(html)
        assertFalse("Should not prefix description with --", html!!.contains("-- Unique customer identifier"))
    }

    fun testDocumentationModelNameIsGrayed() {
        val target = createTarget(modelName = "stg_customers")
        val html = getDocumentationHtml(target)
        assertNotNull(html)
        assertTrue("Model info should use grayed styling", html!!.contains("grayed"))
        assertTrue("Should contain Model: prefix", html.contains("Model: stg_customers"))
    }

    fun testDocumentationDefinedInIsGrayed() {
        val target = createTarget(schemaFilePath = "dbt/models/staging/schema.yml")
        val html = getDocumentationHtml(target)
        assertNotNull(html)
        assertTrue("Should contain Defined in: prefix", html!!.contains("Defined in: dbt/models/staging/schema.yml"))
    }

    fun testDocumentationOmitsDescriptionWhenNull() {
        val target = createTarget(columnDescription = null)
        val html = getDocumentationHtml(target)
        assertNotNull(html)
        // Should still contain model/file info even without description
        assertTrue("Should still contain model name", html!!.contains("Model:"))
        assertTrue("Should still contain defined in", html.contains("Defined in:"))
    }

    fun testDocumentationOmitsDescriptionWhenBlank() {
        val target = createTarget(columnDescription = "   ")
        val html = getDocumentationHtml(target)
        assertNotNull(html)
        // Should still contain the content section with model/file info
        assertTrue("Should contain content section", html!!.contains("class=\"content\""))
        assertTrue("Should still contain model name", html.contains("Model:"))
    }

    // -- computeDocumentation: special characters --

    fun testDocumentationHandlesApostropheInDescription() {
        val target = createTarget(columnDescription = "Date (UTC) of a customer's most recent order")
        val html = getDocumentationHtml(target)
        assertNotNull(html)
        // Should be properly escaped by HtmlChunk.text(), not double-escaped
        assertTrue("Should contain the description", html!!.contains("customer"))
        assertFalse("Should not contain double-escaped entities", html.contains("&amp;"))
    }

    fun testDocumentationHandlesAngleBracketsInDescription() {
        val target = createTarget(columnDescription = "Value must be <100")
        val html = getDocumentationHtml(target)
        assertNotNull(html)
        // HtmlChunk.text() should escape < to &lt;
        assertTrue("Should escape angle brackets", html!!.contains("&lt;"))
    }

    // -- computeDocumentation: structure --

    fun testDocumentationContainsDefinitionSection() {
        val target = createTarget()
        val html = getDocumentationHtml(target)
        assertNotNull(html)
        assertTrue("Should contain definition section", html!!.contains("class=\"definition\""))
    }

    fun testDocumentationContainsContentSection() {
        val target = createTarget()
        val html = getDocumentationHtml(target)
        assertNotNull(html)
        assertTrue("Should contain content section", html!!.contains("class=\"content\""))
    }

    fun testDocumentationIsWrappedInHtmlBody() {
        val target = createTarget()
        val html = getDocumentationHtml(target)
        assertNotNull(html)
        assertTrue("Should be wrapped in html body", html!!.contains("<html>") || html.contains("<body>"))
    }

    // -- navigatable --

    fun testNavigatableIsNullWithoutYamlElement() {
        val target = createTarget(yamlElement = null)
        assertNull("Should return null navigatable without YAML element", target.navigatable)
    }

    fun testNavigatableIsNonNullWithYamlElement() {
        val yamlElement = createYamlElement()
        if (yamlElement == null) return

        val target = createTarget(yamlElement = yamlElement)
        assertNotNull("Should return non-null navigatable with YAML element", target.navigatable)
        assertTrue("Navigatable should be a Navigatable instance", target.navigatable is Navigatable)
    }

    // -- createPointer --

    fun testCreatePointerReturnsNonNull() {
        val target = createTarget()
        val pointer = target.createPointer()
        assertNotNull(pointer)
    }

    fun testPointerDereferenceReturnsTarget() {
        val target = createTarget()
        val pointer = target.createPointer()
        val dereferenced = pointer.dereference()
        assertNotNull("Dereferenced pointer should return a target", dereferenced)
    }

    fun testPointerDereferencedTargetHasSamePresentation() {
        val target = createTarget(columnName = "order_id", columnDataType = null)
        val pointer = target.createPointer()
        val dereferenced = pointer.dereference()
        assertNotNull(dereferenced)
        assertEquals(
            "order_id",
            dereferenced!!.computePresentation().presentableText
        )
    }

    fun testPointerDereferencedTargetPreservesDataType() {
        val target = createTarget(columnName = "order_id", columnDataType = "integer")
        val pointer = target.createPointer()
        val dereferenced = pointer.dereference()
        assertNotNull(dereferenced)
        assertEquals(
            "order_id: integer",
            dereferenced!!.computePresentation().presentableText
        )
    }

    // -- Helpers --

    private fun createTarget(
        columnName: String = "customer_id",
        columnDescription: String? = "Unique customer identifier",
        columnDataType: String? = null,
        modelName: String = "stg_customers",
        modelDescription: String? = "Staging customers table",
        schemaFilePath: String = "dbt/models/staging/schema.yml",
        yamlElement: com.intellij.psi.PsiElement? = null
    ): DatamancerColumnDocumentationTarget {
        val file = myFixture.configureByText("test.txt", "placeholder")
        val element = file.findElementAt(0)!!
        return DatamancerColumnDocumentationTarget(
            element = element,
            project = project,
            columnName = columnName,
            columnDescription = columnDescription,
            columnDataType = columnDataType,
            modelName = modelName,
            modelDescription = modelDescription,
            schemaFilePath = schemaFilePath,
            yamlElement = yamlElement
        )
    }

    /**
     * Creates a YAML PSI element to use as a navigatable target.
     * Returns null if YAML PSI is not available in the test environment.
     */
    private fun createYamlElement(): com.intellij.psi.PsiElement? {
        val yaml = "models:\n  - name: customers\n    columns:\n      - name: customer_id"
        val psiFile = myFixture.configureByText("schema.yml", yaml)
        if (psiFile !is YAMLFile) return null
        return psiFile.findElementAt(10) // somewhere inside the YAML content
    }

    private fun getDocumentationHtml(target: DatamancerColumnDocumentationTarget): String? {
        val result = target.computeDocumentation()
        // DocumentationData is @VisibleForTesting and exposes the html property
        return (result as? DocumentationData)?.html
    }
}

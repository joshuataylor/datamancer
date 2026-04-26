package com.github.joshuataylor.datamancer.core.services

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.yaml.psi.YAMLFile

/**
 * Tests for DbtColumnMetadataService.
 * Tests service lifecycle, YAML parsing, and column metadata extraction.
 */
class DbtColumnMetadataServiceTest : BasePlatformTestCase() {

    // -- Service instantiation tests --

    fun testServiceInstanceCreation() {
        val service = DbtColumnMetadataService.getInstance(project)
        assertNotNull(service)
    }

    fun testServiceIsSingleton() {
        val service1 = DbtColumnMetadataService.getInstance(project)
        val service2 = DbtColumnMetadataService.getInstance(project)
        assertSame(service1, service2)
    }

    fun testServiceExistsInProject() {
        val service = project.getService(DbtColumnMetadataService::class.java)
        assertNotNull(service)
    }

    // -- Empty project tests --

    fun testGetAllModelsWithNoModels() {
        val service = DbtColumnMetadataService.getInstance(project)
        val models = service.getAllModels()
        assertNotNull(models)
        assertTrue("Should return empty map when no dbt project configured", models.isEmpty())
    }

    fun testFindModelNotFound() {
        val service = DbtColumnMetadataService.getInstance(project)
        assertNull(service.findModel("nonexistent_model"))
    }

    fun testFindColumnMetadataNotFound() {
        val service = DbtColumnMetadataService.getInstance(project)
        assertNull(service.findColumnMetadata("nonexistent_model", "nonexistent_column"))
    }

    fun testFindModelWithEmptyName() {
        val service = DbtColumnMetadataService.getInstance(project)
        assertNull(service.findModel(""))
    }

    fun testFindColumnMetadataWithEmptyNames() {
        val service = DbtColumnMetadataService.getInstance(project)
        assertNull(service.findColumnMetadata("", ""))
    }

    // -- YAML parsing: single model with columns --

    fun testParseModelWithColumns() {
        val yaml = """
            models:
              - name: stg_customers
                description: Staging customers table
                columns:
                  - name: customer_id
                    description: Unique customer identifier
                  - name: first_name
                    description: Customer's first name
        """.trimIndent()

        val models = parseYaml(yaml)
        if (models == null) return

        assertEquals(1, models.size)
        val model = models["stg_customers"]
        assertNotNull("Model stg_customers should exist", model)
        assertEquals("stg_customers", model!!.name)
        assertEquals("Staging customers table", model.description)
        assertEquals(2, model.columns.size)

        val customerId = model.columns["customer_id"]
        assertNotNull("Column customer_id should exist", customerId)
        assertEquals("customer_id", customerId!!.name)
        assertEquals("Unique customer identifier", customerId.description)

        val firstName = model.columns["first_name"]
        assertNotNull("Column first_name should exist", firstName)
        assertEquals("Customer's first name", firstName!!.description)
    }

    // -- YAML parsing: multiple models --

    fun testParseMultipleModels() {
        val yaml = """
            models:
              - name: customers
                description: Customer table
                columns:
                  - name: customer_id
                    description: Customer ID
              - name: orders
                description: Order table
                columns:
                  - name: order_id
                    description: Order ID
                  - name: customer_id
                    description: Foreign key to customers
        """.trimIndent()

        val models = parseYaml(yaml)
        if (models == null) return

        assertEquals(2, models.size)
        assertNotNull(models["customers"])
        assertNotNull(models["orders"])
        assertEquals(1, models["customers"]!!.columns.size)
        assertEquals(2, models["orders"]!!.columns.size)
    }

    // -- YAML parsing: model without description --

    fun testParseModelWithoutDescription() {
        val yaml = """
            models:
              - name: stg_orders
                columns:
                  - name: order_id
        """.trimIndent()

        val models = parseYaml(yaml)
        if (models == null) return

        val model = models["stg_orders"]
        assertNotNull(model)
        assertNull("Model description should be null", model!!.description)
    }

    // -- YAML parsing: column without description --

    fun testParseColumnWithoutDescription() {
        val yaml = """
            models:
              - name: stg_orders
                columns:
                  - name: order_id
                    tests:
                      - unique
                      - not_null
        """.trimIndent()

        val models = parseYaml(yaml)
        if (models == null) return

        val column = models["stg_orders"]?.columns?.get("order_id")
        assertNotNull("Column should exist even without description", column)
        assertNull("Column description should be null", column!!.description)
    }

    // -- YAML parsing: model without columns --

    fun testParseModelWithoutColumns() {
        val yaml = """
            models:
              - name: stg_payments
                description: Staging payments
        """.trimIndent()

        val models = parseYaml(yaml)
        if (models == null) return

        val model = models["stg_payments"]
        assertNotNull(model)
        assertEquals("Staging payments", model!!.description)
        assertTrue("Should have empty columns map", model.columns.isEmpty())
    }

    // -- YAML parsing: non-models YAML --

    fun testParseYamlWithoutModelsKey() {
        val yaml = """
            sources:
              - name: raw_data
                tables:
                  - name: customers
        """.trimIndent()

        val models = parseYaml(yaml)
        if (models == null) return

        assertTrue("Should return empty for YAML without models key", models.isEmpty())
    }

    fun testParseEmptyYaml() {
        val models = parseYaml("version: 2")
        if (models == null) return

        assertTrue("Should return empty for YAML without models", models.isEmpty())
    }

    // -- YAML parsing: quoted description values --

    fun testParseQuotedDescriptions() {
        val yaml = """
            models:
              - name: stg_customers
                description: "This is a quoted description"
                columns:
                  - name: customer_id
                    description: "A quoted column description"
        """.trimIndent()

        val models = parseYaml(yaml)
        if (models == null) return

        val model = models["stg_customers"]
        assertNotNull(model)
        assertEquals("This is a quoted description", model!!.description)
        assertEquals("A quoted column description", model.columns["customer_id"]?.description)
    }

    // -- YAML parsing: description with special characters --

    fun testParseDescriptionWithApostrophe() {
        val yaml = """
            models:
              - name: customers
                columns:
                  - name: most_recent_order
                    description: "Date (UTC) of a customer's most recent order"
        """.trimIndent()

        val models = parseYaml(yaml)
        if (models == null) return

        val column = models["customers"]?.columns?.get("most_recent_order")
        assertNotNull(column)
        assertEquals("Date (UTC) of a customer's most recent order", column!!.description)
    }

    // -- YAML parsing: PSI element references --

    fun testParsedModelHasPsiElement() {
        val yaml = """
            models:
              - name: customers
                columns:
                  - name: customer_id
                    description: Customer ID
        """.trimIndent()

        val models = parseYaml(yaml)
        if (models == null) return

        val model = models["customers"]
        assertNotNull(model)
        assertNotNull("Model should have a PSI element", model!!.psiElement)
        assertNotNull("Model should have a file", model.file)
    }

    fun testParsedColumnHasPsiElement() {
        val yaml = """
            models:
              - name: customers
                columns:
                  - name: customer_id
                    description: Customer ID
        """.trimIndent()

        val models = parseYaml(yaml)
        if (models == null) return

        val column = models["customers"]?.columns?.get("customer_id")
        assertNotNull(column)
        assertNotNull("Column should have a PSI element", column!!.psiElement)
        assertNotNull("Column should have a file", column.file)
    }

    // -- YAML parsing: column data_type --

    fun testParseColumnWithDataType() {
        val yaml = """
            models:
              - name: stg_customers
                columns:
                  - name: customer_id
                    description: Unique customer identifier
                    data_type: text
        """.trimIndent()

        val models = parseYaml(yaml)
        if (models == null) return

        val column = models["stg_customers"]?.columns?.get("customer_id")
        assertNotNull(column)
        assertEquals("text", column!!.dataType)
    }

    fun testParseColumnWithoutDataType() {
        val yaml = """
            models:
              - name: stg_customers
                columns:
                  - name: customer_id
                    description: Unique customer identifier
        """.trimIndent()

        val models = parseYaml(yaml)
        if (models == null) return

        val column = models["stg_customers"]?.columns?.get("customer_id")
        assertNotNull(column)
        assertNull("dataType should be null when not specified", column!!.dataType)
    }

    // -- YAML parsing: columns with tests alongside description --

    fun testParseColumnWithTestsAndDescription() {
        val yaml = """
            models:
              - name: orders
                columns:
                  - name: order_id
                    description: This is a unique identifier for an order
                    tests:
                      - unique
                      - not_null
        """.trimIndent()

        val models = parseYaml(yaml)
        if (models == null) return

        val column = models["orders"]?.columns?.get("order_id")
        assertNotNull(column)
        assertEquals("This is a unique identifier for an order", column!!.description)
    }

    // -- Data class tests --

    fun testColumnMetadataDataClass() {
        val paramNames = DbtColumnMetadataService.ColumnMetadata::class.constructors
            .firstOrNull()?.parameters?.map { it.name }
        assertNotNull(paramNames)
        assertTrue(paramNames!!.contains("name"))
        assertTrue(paramNames.contains("description"))
        assertTrue(paramNames.contains("dataType"))
        assertTrue(paramNames.contains("psiElement"))
        assertTrue(paramNames.contains("file"))
    }

    fun testModelMetadataDataClass() {
        val paramNames = DbtColumnMetadataService.ModelMetadata::class.constructors
            .firstOrNull()?.parameters?.map { it.name }
        assertNotNull(paramNames)
        assertTrue(paramNames!!.contains("name"))
        assertTrue(paramNames.contains("description"))
        assertTrue(paramNames.contains("columns"))
        assertTrue(paramNames.contains("psiElement"))
        assertTrue(paramNames.contains("file"))
    }

    // -- Exception safety --

    fun testServiceHandlesInvalidInputGracefully() {
        val service = DbtColumnMetadataService.getInstance(project)
        assertDoesNotThrow { service.getAllModels() }
        assertDoesNotThrow { service.findModel("any") }
        assertDoesNotThrow { service.findColumnMetadata("any", "any") }
    }

    // -- Helpers --

    /**
     * Parses YAML content via the service's internal parsing method.
     * Returns null if YAML PSI is not available in the test environment.
     */
    private fun parseYaml(yamlContent: String): Map<String, DbtColumnMetadataService.ModelMetadata>? {
        val psiFile = myFixture.configureByText("schema.yml", yamlContent)
        val yamlFile = psiFile as? YAMLFile ?: return null
        val virtualFile = psiFile.virtualFile ?: return null

        val service = DbtColumnMetadataService.getInstance(project)
        val models = service.parseModelsFromYaml(yamlFile, virtualFile)
        return models.associateBy { it.name }
    }

    private fun assertDoesNotThrow(block: () -> Unit) {
        try {
            block()
        } catch (e: Throwable) {
            fail("Expected no exception but got: ${e.message}")
        }
    }
}

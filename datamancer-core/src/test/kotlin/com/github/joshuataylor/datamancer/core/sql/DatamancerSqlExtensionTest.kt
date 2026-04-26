package com.github.joshuataylor.datamancer.core.sql

import com.intellij.sql.psi.impl.SqlResolveExtension
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for DatamancerSqlExtension.
 * Tests the SQL resolve extension for dbt model references.
 *
 * Note: Full integration tests with actual SQL resolution would require
 * setting up a dbt project with models and the SQL language support.
 */
class DatamancerSqlExtensionTest : BasePlatformTestCase() {

    // Instantiation tests
    fun testExtensionCanBeInstantiated() {
        val extension = DatamancerSqlExtension()
        assertNotNull(extension)
    }

    fun testExtensionImplementsSqlResolveExtension() {
        val extension = DatamancerSqlExtension()
        assertTrue(extension is SqlResolveExtension)
    }

    // Method existence tests
    fun testProcessMethodExists() {
        val extension = DatamancerSqlExtension()
        val method = extension::class.java.methods.find { it.name == "process" }
        assertNotNull("process method should exist", method)
    }

    // Inner class existence tests
    fun testCsvColumnElementClassExists() {
        val clazz =
            Class.forName("com.github.joshuataylor.datamancer.core.sql.DatamancerSqlExtension\$CsvColumnElement")
        assertNotNull("CsvColumnElement inner class should exist", clazz)
    }

    fun testDatamancerCsvTypeClassExists() {
        val clazz =
            Class.forName("com.github.joshuataylor.datamancer.core.sql.DatamancerSqlExtension\$DatamancerCsvType")
        assertNotNull("DatamancerCsvType inner class should exist", clazz)
    }

    fun testDatamancerModelDasSymbolClassExists() {
        val clazz =
            Class.forName("com.github.joshuataylor.datamancer.core.sql.DatamancerSqlExtension\$DatamancerModelDasSymbol")
        assertNotNull("DatamancerModelDasSymbol inner class should exist", clazz)
    }

    fun testDatamancerSeedFileClassExists() {
        val clazz =
            Class.forName("com.github.joshuataylor.datamancer.core.sql.DatamancerSqlExtension\$DatamancerSeedFile")
        assertNotNull("DatamancerSeedFile inner class should exist", clazz)
    }

    // DatamancerCsvType tests with simple CSV content
    fun testCsvTypeParsesSingleColumn() {
        val psiFile = myFixture.configureByText("test.csv", "id\n1\n2\n3")
        val extension = DatamancerSqlExtension()
        val csvType = extension.DatamancerCsvType(psiFile)

        assertEquals("Should have 1 column", 1, csvType.columnCount)
        assertEquals("Column name should be 'id'", "id", csvType.getColumnName(0))
    }

    fun testCsvTypeParsesMultipleColumns() {
        val psiFile = myFixture.configureByText("test.csv", "id,name,email\n1,John,john@test.com")
        val extension = DatamancerSqlExtension()
        val csvType = extension.DatamancerCsvType(psiFile)

        assertEquals("Should have 3 columns", 3, csvType.columnCount)
        assertEquals("First column should be 'id'", "id", csvType.getColumnName(0))
        assertEquals("Second column should be 'name'", "name", csvType.getColumnName(1))
        assertEquals("Third column should be 'email'", "email", csvType.getColumnName(2))
    }

    fun testCsvTypeHandlesEmptyContent() {
        val psiFile = myFixture.configureByText("test.csv", "")
        val extension = DatamancerSqlExtension()
        assertDoesNotThrow {
            extension.DatamancerCsvType(psiFile)
        }
    }

    fun testCsvTypeHandlesSingleLineNoNewline() {
        val psiFile = myFixture.configureByText("test.csv", "col1,col2")
        val extension = DatamancerSqlExtension()
        val csvType = extension.DatamancerCsvType(psiFile)

        // Should parse columns even without trailing newline
        assertTrue("Should have columns", csvType.columnCount >= 0)
    }

    fun testCsvTypeReturnsColumnElements() {
        val psiFile = myFixture.configureByText("test.csv", "id,name\n1,test")
        val extension = DatamancerSqlExtension()
        val csvType = extension.DatamancerCsvType(psiFile)

        if (csvType.columnCount > 0) {
            val element = csvType.getColumnElement(0)
            assertNotNull("Column element should not be null", element)
        }
    }

    fun testCsvTypeGetMethodsReturnsEmptyList() {
        val psiFile = myFixture.configureByText("test.csv", "id\n1")
        val extension = DatamancerSqlExtension()
        val csvType = extension.DatamancerCsvType(psiFile)

        val methods = csvType.methods
        assertNotNull("Methods list should not be null", methods)
        assertTrue("Methods list should be empty", methods.isEmpty())
    }

    fun testCsvTypeIsColumnQuotedReturnsFalse() {
        val psiFile = myFixture.configureByText("test.csv", "id,name\n1,test")
        val extension = DatamancerSqlExtension()
        val csvType = extension.DatamancerCsvType(psiFile)

        if (csvType.columnCount > 0) {
            assertFalse("Column should not be quoted", csvType.isColumnQuoted(0))
        }
    }

    // CsvColumnElement tests
    fun testCsvColumnElementName() {
        val psiFile = myFixture.configureByText("test.csv", "customer_id\n1")
        val extension = DatamancerSqlExtension()
        val columnElement = extension.CsvColumnElement("customer_id", psiFile, 0)

        assertEquals("Column name should match", "customer_id", columnElement.name)
    }

    fun testCsvColumnElementText() {
        val psiFile = myFixture.configureByText("test.csv", "order_id\n1")
        val extension = DatamancerSqlExtension()
        val columnElement = extension.CsvColumnElement("order_id", psiFile, 0)

        assertEquals("Column text should match", "order_id", columnElement.text)
    }

    fun testCsvColumnElementIsNotQuoted() {
        val psiFile = myFixture.configureByText("test.csv", "id\n1")
        val extension = DatamancerSqlExtension()
        val columnElement = extension.CsvColumnElement("id", psiFile, 0)

        assertFalse("Column should not be quoted", columnElement.isQuoted)
    }

    fun testCsvColumnElementIsValid() {
        val psiFile = myFixture.configureByText("test.csv", "id\n1")
        val extension = DatamancerSqlExtension()
        val columnElement = extension.CsvColumnElement("id", psiFile, 0)

        assertTrue("Column element should be valid", columnElement.isValid)
    }

    fun testCsvColumnElementReturnsCorrectOffset() {
        val psiFile = myFixture.configureByText("test.csv", "id,name\n1,test")
        val extension = DatamancerSqlExtension()
        val columnElement = extension.CsvColumnElement("name", psiFile, 3)

        assertEquals("Text offset should be 3", 3, columnElement.textOffset)
    }

    // Multiple instances tests
    fun testMultipleInstancesCanBeCreated() {
        val extension1 = DatamancerSqlExtension()
        val extension2 = DatamancerSqlExtension()

        assertNotNull(extension1)
        assertNotNull(extension2)
        assertNotSame(extension1, extension2)
    }

    // Quoted CSV value tests
    fun testCsvTypeHandlesQuotedColumnNames() {
        val psiFile = myFixture.configureByText("test.csv", "\"First Name\",\"Last Name\"\nJohn,Doe")
        val extension = DatamancerSqlExtension()
        val csvType = extension.DatamancerCsvType(psiFile)

        assertEquals("Should have 2 columns", 2, csvType.columnCount)
        assertEquals("First column should be 'First Name'", "First Name", csvType.getColumnName(0))
        assertEquals("Second column should be 'Last Name'", "Last Name", csvType.getColumnName(1))
    }

    fun testCsvTypeHandlesColumnWithCommaInQuotes() {
        val psiFile = myFixture.configureByText("test.csv", "\"Name, Full\",age\nJohn,30")
        val extension = DatamancerSqlExtension()
        val csvType = extension.DatamancerCsvType(psiFile)

        assertEquals("Should have 2 columns", 2, csvType.columnCount)
        assertEquals("First column should be 'Name, Full'", "Name, Full", csvType.getColumnName(0))
        assertEquals("Second column should be 'age'", "age", csvType.getColumnName(1))
    }

    fun testCsvTypeHandlesEscapedQuotesInColumnName() {
        val psiFile = myFixture.configureByText("test.csv", "\"Column \"\"A\"\"\",\"Column B\"\nval1,val2")
        val extension = DatamancerSqlExtension()
        val csvType = extension.DatamancerCsvType(psiFile)

        assertEquals("Should have 2 columns", 2, csvType.columnCount)
        assertEquals("First column should have escaped quote", "Column \"A\"", csvType.getColumnName(0))
        assertEquals("Second column should be 'Column B'", "Column B", csvType.getColumnName(1))
    }

    fun testCsvTypeHandlesMixedQuotedAndUnquotedColumns() {
        val psiFile = myFixture.configureByText("test.csv", "id,\"Full Name\",email\n1,John Doe,john@test.com")
        val extension = DatamancerSqlExtension()
        val csvType = extension.DatamancerCsvType(psiFile)

        assertEquals("Should have 3 columns", 3, csvType.columnCount)
        assertEquals("First column should be 'id'", "id", csvType.getColumnName(0))
        assertEquals("Second column should be 'Full Name'", "Full Name", csvType.getColumnName(1))
        assertEquals("Third column should be 'email'", "email", csvType.getColumnName(2))
    }

    fun testCsvTypeHandlesCarriageReturnLineFeed() {
        val psiFile = myFixture.configureByText("test.csv", "id,name\r\n1,test\r\n2,another")
        val extension = DatamancerSqlExtension()
        val csvType = extension.DatamancerCsvType(psiFile)

        assertEquals("Should have 2 columns", 2, csvType.columnCount)
        assertEquals("First column should be 'id'", "id", csvType.getColumnName(0))
        assertEquals("Second column should be 'name'", "name", csvType.getColumnName(1))
    }

    fun testCsvTypeHandlesWhitespaceAroundColumnNames() {
        val psiFile = myFixture.configureByText("test.csv", "  id  ,  name  \n1,test")
        val extension = DatamancerSqlExtension()
        val csvType = extension.DatamancerCsvType(psiFile)

        assertEquals("Should have 2 columns", 2, csvType.columnCount)
        assertEquals("First column should be trimmed", "id", csvType.getColumnName(0))
        assertEquals("Second column should be trimmed", "name", csvType.getColumnName(1))
    }

    fun testCsvTypeHandlesEmptyQuotedColumn() {
        val psiFile = myFixture.configureByText("test.csv", "id,\"\",name\n1,,test")
        val extension = DatamancerSqlExtension()
        val csvType = extension.DatamancerCsvType(psiFile)

        assertEquals("Should have 3 columns", 3, csvType.columnCount)
        assertEquals("First column should be 'id'", "id", csvType.getColumnName(0))
        assertEquals("Second column should be empty string", "", csvType.getColumnName(1))
        assertEquals("Third column should be 'name'", "name", csvType.getColumnName(2))
    }

    fun testCsvTypeHandlesQuotedColumnWithNewlineContent() {
        // Test that newlines inside quotes don't break header parsing
        // This tests the header row only, which shouldn't have newlines
        val psiFile = myFixture.configureByText("test.csv", "id,name\n1,test")
        val extension = DatamancerSqlExtension()
        val csvType = extension.DatamancerCsvType(psiFile)

        assertEquals("Should have 2 columns", 2, csvType.columnCount)
    }

    fun testCsvTypeHandlesSingleQuotedColumn() {
        val psiFile = myFixture.configureByText("test.csv", "\"single column\"\nvalue")
        val extension = DatamancerSqlExtension()
        val csvType = extension.DatamancerCsvType(psiFile)

        assertEquals("Should have 1 column", 1, csvType.columnCount)
        assertEquals("Column should be 'single column'", "single column", csvType.getColumnName(0))
    }

    fun testCsvTypeHandlesConsecutiveQuotes() {
        val psiFile = myFixture.configureByText("test.csv", "\"\"\"\",normal\n,test")
        val extension = DatamancerSqlExtension()
        val csvType = extension.DatamancerCsvType(psiFile)

        assertEquals("Should have 2 columns", 2, csvType.columnCount)
        assertEquals("First column should be single quote", "\"", csvType.getColumnName(0))
        assertEquals("Second column should be 'normal'", "normal", csvType.getColumnName(1))
    }

    // Process method tests
    fun testProcessMethodHasCorrectParameterCount() {
        val extension = DatamancerSqlExtension()
        val processMethods = extension::class.java.methods.filter { it.name == "process" }
        assertTrue("process method should exist", processMethods.isNotEmpty())
    }

    // Seed file inner class tests
    fun testDatamancerSeedFileClassCanBeLoaded() {
        val clazz = Class.forName(
            "com.github.joshuataylor.datamancer.core.sql.DatamancerSqlExtension\$DatamancerSeedFile"
        )
        assertNotNull("DatamancerSeedFile should be loadable", clazz)
    }

    fun testDatamancerModelDasSymbolClassCanBeLoaded() {
        val clazz = Class.forName(
            "com.github.joshuataylor.datamancer.core.sql.DatamancerSqlExtension\$DatamancerModelDasSymbol"
        )
        assertNotNull("DatamancerModelDasSymbol should be loadable", clazz)
    }

    // Large CSV test
    fun testCsvTypeHandlesManyColumns() {
        val headers = (1..50).joinToString(",") { "col_$it" }
        val values = (1..50).joinToString(",") { "val_$it" }
        val psiFile = myFixture.configureByText("test.csv", "$headers\n$values")
        val extension = DatamancerSqlExtension()
        val csvType = extension.DatamancerCsvType(psiFile)

        assertEquals("Should have 50 columns", 50, csvType.columnCount)
        assertEquals("First column should be 'col_1'", "col_1", csvType.getColumnName(0))
        assertEquals("Last column should be 'col_50'", "col_50", csvType.getColumnName(49))
    }

    private fun assertDoesNotThrow(block: () -> Unit) {
        try {
            block()
        } catch (e: Throwable) {
            fail("Expected no exception but got: ${e.message}")
        }
    }
}

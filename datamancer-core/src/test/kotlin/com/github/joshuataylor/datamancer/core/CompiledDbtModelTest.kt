package com.github.joshuataylor.datamancer.core

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for CompiledDbtModel data class.
 * Tests data class functionality.
 *
 * Note: Serialization tests are skipped as they require the kotlinx.serialization
 * compiler plugin which may not be fully available in the test environment.
 */
class CompiledDbtModelTest : BasePlatformTestCase() {

    // Data class instantiation tests
    fun testDataClassCanBeInstantiated() {
        val model = CompiledDbtModel(
            success = true,
            compiledCode = "SELECT 1",
            compiledPath = "target/compiled/model.sql",
            compiledFullPath = "/Users/test/project/target/compiled/model.sql"
        )
        assertNotNull(model)
    }

    fun testDataClassProperties() {
        val model = CompiledDbtModel(
            success = true,
            compiledCode = "SELECT * FROM customers",
            compiledPath = "target/compiled/stg_customers.sql",
            compiledFullPath = "/full/path/target/compiled/stg_customers.sql"
        )

        assertTrue(model.success)
        assertEquals("SELECT * FROM customers", model.compiledCode)
        assertEquals("target/compiled/stg_customers.sql", model.compiledPath)
        assertEquals("/full/path/target/compiled/stg_customers.sql", model.compiledFullPath)
        assertNull(model.error)
    }

    // Equality tests
    fun testEquality() {
        val model1 = CompiledDbtModel(
            success = true,
            compiledCode = "SELECT 1",
            compiledPath = "path",
            compiledFullPath = "/full/path"
        )
        val model2 = CompiledDbtModel(
            success = true,
            compiledCode = "SELECT 1",
            compiledPath = "path",
            compiledFullPath = "/full/path"
        )

        assertEquals(model1, model2)
        assertEquals(model1.hashCode(), model2.hashCode())
    }

    fun testInequality() {
        val model1 = CompiledDbtModel(
            success = true,
            compiledCode = "SELECT 1",
            compiledPath = "path1",
            compiledFullPath = "/full/path1"
        )
        val model2 = CompiledDbtModel(
            success = true,
            compiledCode = "SELECT 2",
            compiledPath = "path2",
            compiledFullPath = "/full/path2"
        )

        assertFalse("Different models should not be equal", model1 == model2)
    }

    fun testInequalityByCompiledCode() {
        val model1 = CompiledDbtModel(
            success = true,
            compiledCode = "SELECT 1",
            compiledPath = "path",
            compiledFullPath = "/path"
        )
        val model2 = CompiledDbtModel(
            success = true,
            compiledCode = "SELECT 2",
            compiledPath = "path",
            compiledFullPath = "/path"
        )

        assertFalse("Models with different compiledCode should not be equal", model1 == model2)
    }

    fun testInequalityByCompiledPath() {
        val model1 = CompiledDbtModel(
            success = true,
            compiledCode = "SELECT 1",
            compiledPath = "path1",
            compiledFullPath = "/path"
        )
        val model2 = CompiledDbtModel(
            success = true,
            compiledCode = "SELECT 1",
            compiledPath = "path2",
            compiledFullPath = "/path"
        )

        assertFalse("Models with different compiledPath should not be equal", model1 == model2)
    }

    fun testInequalityBySuccess() {
        val model1 = CompiledDbtModel(
            success = true,
            compiledCode = "SELECT 1",
            compiledPath = "path",
            compiledFullPath = "/path"
        )
        val model2 = CompiledDbtModel(
            success = false,
            compiledCode = "SELECT 1",
            compiledPath = "path",
            compiledFullPath = "/path"
        )

        assertFalse("Models with different success should not be equal", model1 == model2)
    }

    fun testInequalityByError() {
        val model1 = CompiledDbtModel(
            success = false,
            error = "Error A"
        )
        val model2 = CompiledDbtModel(
            success = false,
            error = "Error B"
        )

        assertFalse("Models with different error should not be equal", model1 == model2)
    }

    // Copy tests
    fun testCopyWithModification() {
        val original = CompiledDbtModel(
            success = true,
            compiledCode = "SELECT 1",
            compiledPath = "original/path",
            compiledFullPath = "/original/full/path"
        )

        val modified = original.copy(compiledCode = "SELECT 2")

        assertTrue(modified.success)
        assertEquals("SELECT 2", modified.compiledCode)
        assertEquals(original.compiledPath, modified.compiledPath)
        assertEquals(original.compiledFullPath, modified.compiledFullPath)
        assertNull(modified.error)
    }

    fun testCopyPreservesUnchangedFields() {
        val original = CompiledDbtModel(
            success = true,
            compiledCode = "SELECT 1",
            compiledPath = "path",
            compiledFullPath = "/full/path"
        )

        val copy = original.copy()

        assertEquals(original, copy)
        assertNotSame("Copy should be a different instance", original, copy)
    }

    fun testCopyChangesSuccess() {
        val original = CompiledDbtModel(
            success = true,
            compiledCode = "SELECT 1",
            compiledPath = "path",
            compiledFullPath = "/full/path"
        )

        val failed = original.copy(success = false, error = "Something went wrong")

        assertFalse(failed.success)
        assertEquals("SELECT 1", failed.compiledCode)
        assertEquals("Something went wrong", failed.error)
    }

    // Edge cases
    fun testEmptyStrings() {
        val model = CompiledDbtModel(
            success = true,
            compiledCode = "",
            compiledPath = "",
            compiledFullPath = ""
        )

        assertNotNull(model)
        assertEquals("", model.compiledCode)
        assertEquals("", model.compiledPath)
        assertEquals("", model.compiledFullPath)
    }

    fun testMultilineCompiledCode() {
        val multilineCode = """
            SELECT
                id,
                name,
                created_at
            FROM customers
            WHERE active = true
        """.trimIndent()

        val model = CompiledDbtModel(
            success = true,
            compiledCode = multilineCode,
            compiledPath = "target/model.sql",
            compiledFullPath = "/path/target/model.sql"
        )

        assertTrue(model.compiledCode!!.contains("SELECT"))
        assertTrue(model.compiledCode!!.contains("FROM customers"))
    }

    fun testSpecialCharactersInCode() {
        val codeWithSpecialChars = "SELECT * FROM {{ ref('model') }} WHERE name = 'O''Brien'"

        val model = CompiledDbtModel(
            success = true,
            compiledCode = codeWithSpecialChars,
            compiledPath = "path",
            compiledFullPath = "/path"
        )

        assertEquals(codeWithSpecialChars, model.compiledCode)
    }

    fun testUnicodeInCode() {
        val codeWithUnicode = "SELECT * FROM customers WHERE name = 'Müller'"

        val model = CompiledDbtModel(
            success = true,
            compiledCode = codeWithUnicode,
            compiledPath = "path",
            compiledFullPath = "/path"
        )

        assertEquals(codeWithUnicode, model.compiledCode)
    }

    fun testLongCompiledCode() {
        val longCode = "SELECT " + (1..100).joinToString(", ") { "col$it" } + " FROM large_table"

        val model = CompiledDbtModel(
            success = true,
            compiledCode = longCode,
            compiledPath = "path",
            compiledFullPath = "/path"
        )

        assertTrue(model.compiledCode!!.length > 500)
        assertTrue(model.compiledCode!!.startsWith("SELECT"))
    }

    // Success and error field tests
    fun testSuccessField() {
        val successModel = CompiledDbtModel(
            success = true,
            compiledCode = "SELECT 1",
            compiledPath = "path",
            compiledFullPath = "/full/path"
        )
        assertTrue(successModel.success)

        val failureModel = CompiledDbtModel(
            success = false,
            error = "Compilation failed"
        )
        assertFalse(failureModel.success)
    }

    fun testErrorField() {
        val modelWithError = CompiledDbtModel(
            success = false,
            error = "Model not found: stg_missing"
        )

        assertFalse(modelWithError.success)
        assertEquals("Model not found: stg_missing", modelWithError.error)
        assertNull(modelWithError.compiledCode)
        assertNull(modelWithError.compiledPath)
        assertNull(modelWithError.compiledFullPath)
    }

    fun testDefaultNullableFields() {
        val model = CompiledDbtModel(success = true)

        assertTrue(model.success)
        assertNull(model.compiledCode)
        assertNull(model.compiledPath)
        assertNull(model.compiledFullPath)
        assertNull(model.error)
    }

    fun testErrorModelEquality() {
        val error1 = CompiledDbtModel(
            success = false,
            error = "Compilation failed"
        )
        val error2 = CompiledDbtModel(
            success = false,
            error = "Compilation failed"
        )

        assertEquals(error1, error2)
        assertEquals(error1.hashCode(), error2.hashCode())
    }

    fun testErrorWithPartialFields() {
        val model = CompiledDbtModel(
            success = false,
            compiledCode = "SELECT 1",
            error = "Partial compilation failure"
        )

        assertFalse(model.success)
        assertEquals("SELECT 1", model.compiledCode)
        assertNull(model.compiledPath)
        assertNull(model.compiledFullPath)
        assertEquals("Partial compilation failure", model.error)
    }

    // toString tests
    fun testToStringContainsAllFields() {
        val model = CompiledDbtModel(
            success = true,
            compiledCode = "SELECT 1",
            compiledPath = "target/path",
            compiledFullPath = "/full/target/path"
        )

        val str = model.toString()
        assertTrue("toString should contain success", str.contains("success=true"))
        assertTrue("toString should contain compiledCode", str.contains("SELECT 1"))
        assertTrue("toString should contain compiledPath", str.contains("target/path"))
        assertTrue("toString should contain compiledFullPath", str.contains("/full/target/path"))
        assertTrue("toString should contain error", str.contains("error=null"))
    }

    fun testErrorModelToString() {
        val model = CompiledDbtModel(
            success = false,
            error = "dbt is not installed"
        )

        val str = model.toString()
        assertTrue("toString should contain success=false", str.contains("success=false"))
        assertTrue("toString should contain error message", str.contains("dbt is not installed"))
    }

    // Component functions (destructuring)
    fun testDestructuring() {
        val model = CompiledDbtModel(
            success = true,
            compiledCode = "SELECT 1",
            compiledPath = "path",
            compiledFullPath = "/full/path"
        )

        val (success, code, path, fullPath, error) = model

        assertTrue(success)
        assertEquals("SELECT 1", code)
        assertEquals("path", path)
        assertEquals("/full/path", fullPath)
        assertNull(error)
    }
}

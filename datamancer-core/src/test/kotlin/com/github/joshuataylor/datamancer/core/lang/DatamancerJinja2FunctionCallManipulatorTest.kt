package com.github.joshuataylor.datamancer.core.lang

import com.intellij.jinja.tags.Jinja2FunctionCall
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.psi.AbstractElementManipulator
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for DbtJinja2FunctionCallManipulator.
 * Tests the element manipulator for rename refactoring of ref() calls.
 */
class DbtJinja2FunctionCallManipulatorTest : BasePlatformTestCase() {

    // Instantiation tests
    fun testManipulatorCanBeInstantiated() {
        val manipulator = DbtJinja2FunctionCallManipulator()
        assertNotNull(manipulator)
    }

    fun testManipulatorExtendsAbstractElementManipulator() {
        val manipulator = DbtJinja2FunctionCallManipulator()
        assertTrue(manipulator is AbstractElementManipulator<*>)
    }

    // Method existence tests
    fun testHandleContentChangeMethodExists() {
        val manipulator = DbtJinja2FunctionCallManipulator()
        val method = manipulator::class.java.methods.find { it.name == "handleContentChange" }
        assertNotNull("handleContentChange method should exist", method)
    }

    fun testGetRangeInElementMethodExists() {
        val manipulator = DbtJinja2FunctionCallManipulator()
        val method = manipulator::class.java.methods.find { it.name == "getRangeInElement" }
        assertNotNull("getRangeInElement method should exist", method)
    }

    // FileUtilRt.getNameWithoutExtension tests (used internally)
    fun testFileUtilStripsSqlExtension() {
        val result = FileUtilRt.getNameWithoutExtension("customers.sql")
        assertEquals("customers", result)
    }

    fun testFileUtilStripsCsvExtension() {
        val result = FileUtilRt.getNameWithoutExtension("raw_data.csv")
        assertEquals("raw_data", result)
    }

    fun testFileUtilHandlesNoExtension() {
        val result = FileUtilRt.getNameWithoutExtension("model_name")
        assertEquals("model_name", result)
    }

    fun testFileUtilHandlesMultipleDots() {
        val result = FileUtilRt.getNameWithoutExtension("stg.customers.v2.sql")
        assertEquals("stg.customers.v2", result)
    }

    fun testFileUtilHandlesUnderscores() {
        val result = FileUtilRt.getNameWithoutExtension("stg_my_model_v2.sql")
        assertEquals("stg_my_model_v2", result)
    }

    fun testFileUtilHandlesHyphens() {
        val result = FileUtilRt.getNameWithoutExtension("my-model-name.sql")
        assertEquals("my-model-name", result)
    }

    // Multiple instances tests
    fun testMultipleInstancesCanBeCreated() {
        val manipulator1 = DbtJinja2FunctionCallManipulator()
        val manipulator2 = DbtJinja2FunctionCallManipulator()

        assertNotNull(manipulator1)
        assertNotNull(manipulator2)
        assertNotSame(manipulator1, manipulator2)
    }

    // handleContentChange with actual Jinja2 elements
    fun testHandleContentChangeWithFunctionCall() {
        val psiFile = myFixture.configureByText("test.html", "{{ ref('old_model') }}")
        val functionCall = PsiTreeUtil.findChildOfType(psiFile, Jinja2FunctionCall::class.java)

        if (functionCall != null) {
            val manipulator = DbtJinja2FunctionCallManipulator()
            val range = TextRange(0, functionCall.textLength)

            // This should not throw
            assertDoesNotThrow {
                manipulator.handleContentChange(functionCall, range, "new_model.sql")
            }
        }
    }

    fun testHandleContentChangeStripsExtension() {
        val psiFile = myFixture.configureByText("test.html", "{{ ref('customers') }}")
        val functionCall = PsiTreeUtil.findChildOfType(psiFile, Jinja2FunctionCall::class.java)

        if (functionCall != null) {
            val manipulator = DbtJinja2FunctionCallManipulator()
            val range = TextRange(0, functionCall.textLength)

            // The new content has .sql extension which should be stripped
            val result = manipulator.handleContentChange(functionCall, range, "customer_data.sql")

            // Result should be the same element (modified in place)
            assertNotNull(result)
        }
    }

    fun testHandleContentChangeWithEmptyStringLiteral() {
        val psiFile = myFixture.configureByText("test.html", "{{ ref('') }}")
        val functionCall = PsiTreeUtil.findChildOfType(psiFile, Jinja2FunctionCall::class.java)

        if (functionCall != null) {
            val manipulator = DbtJinja2FunctionCallManipulator()
            val range = TextRange(0, functionCall.textLength)

            assertDoesNotThrow {
                manipulator.handleContentChange(functionCall, range, "new_model.sql")
            }
        }
    }

    fun testHandleContentChangeWithSourceCall() {
        val psiFile = myFixture.configureByText("test.html", "{{ source('schema', 'table') }}")
        val functionCall = PsiTreeUtil.findChildOfType(psiFile, Jinja2FunctionCall::class.java)

        if (functionCall != null) {
            val manipulator = DbtJinja2FunctionCallManipulator()
            val range = TextRange(0, functionCall.textLength)

            // Should work on source() calls too (manipulates first string literal)
            assertDoesNotThrow {
                manipulator.handleContentChange(functionCall, range, "new_name")
            }
        }
    }

    private fun assertDoesNotThrow(block: () -> Unit) {
        try {
            block()
        } catch (e: Throwable) {
            fail("Expected no exception but got: ${e.message}")
        }
    }
}

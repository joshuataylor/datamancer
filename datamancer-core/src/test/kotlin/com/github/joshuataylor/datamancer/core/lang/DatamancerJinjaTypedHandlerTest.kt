package com.github.joshuataylor.datamancer.core.lang

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for DbtJinjaTypedHandler.
 * Tests automatic quote completion in Jinja2 templates.
 *
 * Note: Full integration tests with actual quote completion would require
 * Jinja2 file type to be properly registered in the test environment.
 */
class DbtJinjaTypedHandlerTest : BasePlatformTestCase() {

    // Instantiation tests
    fun testHandlerCanBeInstantiated() {
        val handler = DbtJinjaTypedHandler()
        assertNotNull(handler)
    }

    fun testHandlerExtendsTypedHandlerDelegate() {
        val handler = DbtJinjaTypedHandler()
        assertTrue(handler is TypedHandlerDelegate)
    }

    // Method existence tests
    fun testCharTypedMethodExists() {
        val handler = DbtJinjaTypedHandler()
        val method = handler::class.java.methods.find { it.name == "charTyped" }
        assertNotNull("charTyped method should exist", method)
    }

    // Handler returns Result.CONTINUE for non-Jinja2 files
    fun testCharTypedReturnsContinueForSqlFile() {
        val handler = DbtJinjaTypedHandler()
        val psiFile = myFixture.configureByText("test.sql", "SELECT 1")
        val result = handler.charTyped('\'', project, myFixture.editor, psiFile)
        assertEquals("Should return CONTINUE for SQL files", TypedHandlerDelegate.Result.CONTINUE, result)
    }

    fun testCharTypedReturnsContinueForNonQuoteChar() {
        val handler = DbtJinjaTypedHandler()
        val psiFile = myFixture.configureByText("test.sql", "SELECT 1")
        val result = handler.charTyped('a', project, myFixture.editor, psiFile)
        assertEquals("Should return CONTINUE for non-quote char", TypedHandlerDelegate.Result.CONTINUE, result)
    }

    fun testCharTypedReturnsContinueForTextFile() {
        val handler = DbtJinjaTypedHandler()
        val psiFile = myFixture.configureByText("test.txt", "hello world")
        val result = handler.charTyped('"', project, myFixture.editor, psiFile)
        assertEquals("Should return CONTINUE for TXT files", TypedHandlerDelegate.Result.CONTINUE, result)
    }

    // Multiple instances tests
    fun testMultipleInstancesCanBeCreated() {
        val handler1 = DbtJinjaTypedHandler()
        val handler2 = DbtJinjaTypedHandler()

        assertNotNull(handler1)
        assertNotNull(handler2)
        assertNotSame(handler1, handler2)
    }

    // Edge cases
    fun testHandlerDoesNotThrowOnEmptyFile() {
        val handler = DbtJinjaTypedHandler()
        val psiFile = myFixture.configureByText("test.txt", "")
        assertDoesNotThrow {
            handler.charTyped('\'', project, myFixture.editor, psiFile)
        }
    }

    fun testHandlerDoesNotThrowOnSingleChar() {
        val handler = DbtJinjaTypedHandler()
        val psiFile = myFixture.configureByText("test.txt", "a")
        assertDoesNotThrow {
            handler.charTyped('"', project, myFixture.editor, psiFile)
        }
    }

    fun testHandlerHandlesSingleQuote() {
        val handler = DbtJinjaTypedHandler()
        val psiFile = myFixture.configureByText("test.sql", "SELECT '")
        assertDoesNotThrow {
            handler.charTyped('\'', project, myFixture.editor, psiFile)
        }
    }

    fun testHandlerHandlesDoubleQuote() {
        val handler = DbtJinjaTypedHandler()
        val psiFile = myFixture.configureByText("test.sql", "SELECT \"")
        assertDoesNotThrow {
            handler.charTyped('"', project, myFixture.editor, psiFile)
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

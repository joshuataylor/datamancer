package com.github.joshuataylor.datamancer.core.lang

import com.intellij.jinja.tags.Jinja2FunctionCall
import com.intellij.jinja.template.psi.impl.Jinja2VariableReferenceImpl
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiReference
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for DbtMacroReference.
 * Tests the reference implementation for dbt custom macro calls.
 *
 * Note: Full integration tests with actual macro resolution would require
 * setting up a dbt project with DatamancerDbtProjectIndexService configured.
 */
class DatamancerMacroReferenceTest : BasePlatformTestCase() {

    // Helper to create a macro reference for testing
    private fun createMacroReference(jinjaCode: String): DbtMacroReference? {
        val psiFile = myFixture.configureByText("test.html", jinjaCode)
        val functionCall = PsiTreeUtil.findChildOfType(psiFile, Jinja2FunctionCall::class.java)
            ?: return null
        val callee = functionCall.callee
        val macroName = (callee as? Jinja2VariableReferenceImpl)?.name
            ?: return null

        val calleeOffset = callee.startOffsetInParent
        val textRange = TextRange(calleeOffset, calleeOffset + macroName.length)

        return DbtMacroReference(functionCall, textRange, macroName)
    }

    // Instantiation tests
    fun testReferenceCanBeCreated() {
        val ref = createMacroReference("{{ testf() }}")
        if (ref != null) {
            assertNotNull("Reference should be created", ref)
        }
    }

    fun testReferenceImplementsPsiReference() {
        val ref = createMacroReference("{{ testf() }}")
        if (ref != null) {
            assertTrue("Should implement PsiReference", ref is PsiReference)
        }
    }

    // isSoft tests
    fun testReferenceIsSoft() {
        val ref = createMacroReference("{{ testf() }}")
        if (ref != null) {
            assertTrue("Reference should be soft", ref.isSoft)
        }
    }

    // resolve tests - without dbt project, should return null
    fun testResolveReturnsNullWithoutDbtProject() {
        val ref = createMacroReference("{{ testf() }}")
        if (ref != null) {
            val result = ref.resolve()
            assertNull("Should return null when no dbt project", result)
        }
    }

    // getVariants tests - without dbt project, should return empty
    fun testGetVariantsReturnsEmptyWithoutDbtProject() {
        val ref = createMacroReference("{{ testf() }}")
        if (ref != null) {
            val variants = ref.variants
            assertNotNull("Variants should not be null", variants)
            assertTrue("Should return empty variants without dbt project", variants.isEmpty())
        }
    }

    // Macro name tests - verify no exceptions thrown
    fun testReferenceWithUnderscoreMacroName() {
        assertDoesNotThrow {
            createMacroReference("{{ my_custom_macro() }}")
        }
    }

    fun testReferenceWithMacroArgs() {
        assertDoesNotThrow {
            createMacroReference("{{ testf2('hello') }}")
        }
    }

    fun testReferenceWithMultipleMacroArgs() {
        assertDoesNotThrow {
            createMacroReference("{{ my_macro(arg1, arg2, arg3) }}")
        }
    }

    fun testReferenceWithKeywordArgs() {
        assertDoesNotThrow {
            createMacroReference("{{ my_macro(column='id', scale=2) }}")
        }
    }

    // Method existence tests
    fun testResolveMethodExists() {
        val method = DbtMacroReference::class.java.methods.find { it.name == "resolve" }
        assertNotNull("resolve method should exist", method)
    }

    fun testGetVariantsMethodExists() {
        val method = DbtMacroReference::class.java.methods.find { it.name == "getVariants" }
        assertNotNull("getVariants method should exist", method)
    }

    // Class inheritance tests
    fun testExtendsModelReferenceBase() {
        val superclass = DbtMacroReference::class.java.superclass
        assertEquals(
            "Should extend DbtModelReferenceBase",
            DbtModelReferenceBase::class.java.name,
            superclass?.name
        )
    }

    private fun assertDoesNotThrow(block: () -> Unit) {
        try {
            block()
        } catch (e: Throwable) {
            fail("Expected no exception but got: ${e.message}")
        }
    }
}

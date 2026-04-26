package com.github.joshuataylor.datamancer.core.lang

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for DbtModelReferenceBase.
 * Tests the abstract base class for dbt model references.
 */
class DbtModelReferenceBaseTest : BasePlatformTestCase() {

    // Create a concrete implementation for testing
    private class TestReference(
        element: PsiElement,
        textRange: TextRange,
        modelName: String
    ) : DbtModelReferenceBase<PsiElement>(element, textRange, modelName) {
        override fun resolveInner(incompleteCode: Boolean): Array<ResolveResult> {
            return ResolveResult.EMPTY_ARRAY
        }

        override fun getVariants(): Array<Any> {
            return emptyArray()
        }
    }

    // Class structure tests
    fun testBaseClassExtendsCorrectly() {
        val psiFile = myFixture.configureByText("test.sql", "SELECT 1")
        val ref = TestReference(psiFile, TextRange(0, 5), "test_model")
        assertTrue("Should extend PsiReferenceBase", ref is PsiReferenceBase<*>)
    }

    // isSoft tests
    fun testIsSoftReturnsTrue() {
        val psiFile = myFixture.configureByText("test.sql", "SELECT 1")
        val ref = TestReference(psiFile, TextRange(0, 5), "test_model")
        assertTrue("isSoft should return true", ref.isSoft)
    }

    // resolve tests
    fun testResolveReturnsNullForUnresolvableReference() {
        val psiFile = myFixture.configureByText("test.sql", "SELECT 1")
        val ref = TestReference(psiFile, TextRange(0, 5), "nonexistent_model")
        val result = ref.resolve()
        assertNull("Should return null for unresolvable reference", result)
    }

    // equals and hashCode tests
    fun testEqualsWithSameElementAndName() {
        val psiFile = myFixture.configureByText("test.sql", "SELECT 1")
        val ref1 = TestReference(psiFile, TextRange(0, 5), "model_name")
        val ref2 = TestReference(psiFile, TextRange(0, 5), "model_name")
        assertEquals("References with same element and name should be equal", ref1, ref2)
    }

    fun testEqualsWithDifferentNames() {
        val psiFile = myFixture.configureByText("test.sql", "SELECT 1")
        val ref1 = TestReference(psiFile, TextRange(0, 5), "model_a")
        val ref2 = TestReference(psiFile, TextRange(0, 5), "model_b")
        assertFalse("References with different names should not be equal", ref1 == ref2)
    }

    fun testEqualsWithSameInstance() {
        val psiFile = myFixture.configureByText("test.sql", "SELECT 1")
        val ref = TestReference(psiFile, TextRange(0, 5), "model_name")
        assertEquals("Reference should equal itself", ref, ref)
    }

    fun testEqualsWithNull() {
        val psiFile = myFixture.configureByText("test.sql", "SELECT 1")
        val ref = TestReference(psiFile, TextRange(0, 5), "model_name")
        assertFalse("Reference should not equal null", ref.equals(null))
    }

    fun testEqualsWithDifferentType() {
        val psiFile = myFixture.configureByText("test.sql", "SELECT 1")
        val ref = TestReference(psiFile, TextRange(0, 5), "model_name")
        assertFalse("Reference should not equal string", ref.equals("model_name"))
    }

    fun testHashCodeConsistentWithEquals() {
        val psiFile = myFixture.configureByText("test.sql", "SELECT 1")
        val ref1 = TestReference(psiFile, TextRange(0, 5), "model_name")
        val ref2 = TestReference(psiFile, TextRange(0, 5), "model_name")
        assertEquals("Equal references should have same hashCode", ref1.hashCode(), ref2.hashCode())
    }

    fun testHashCodeDifferentForDifferentNames() {
        val psiFile = myFixture.configureByText("test.sql", "SELECT 1")
        val ref1 = TestReference(psiFile, TextRange(0, 5), "model_a")
        val ref2 = TestReference(psiFile, TextRange(0, 5), "model_b")
        // Not strictly required, but likely
        assertFalse("Different names should likely have different hashCodes", ref1.hashCode() == ref2.hashCode())
    }

    // Element and range tests
    fun testElementIsAccessible() {
        val psiFile = myFixture.configureByText("test.sql", "SELECT 1")
        val ref = TestReference(psiFile, TextRange(0, 5), "test_model")
        assertSame("Element should be accessible", psiFile, ref.element)
    }

    fun testRangeInElementIsAccessible() {
        val psiFile = myFixture.configureByText("test.sql", "SELECT 1")
        val range = TextRange(0, 5)
        val ref = TestReference(psiFile, range, "test_model")
        assertEquals("RangeInElement should be accessible", range, ref.rangeInElement)
    }

    // Method existence tests
    fun testResolveMethodExists() {
        val method = DbtModelReferenceBase::class.java.methods.find { it.name == "resolve" }
        assertNotNull("resolve method should exist", method)
    }

    fun testIsSoftMethodExists() {
        val method = DbtModelReferenceBase::class.java.methods.find { it.name == "isSoft" }
        assertNotNull("isSoft method should exist", method)
    }

    fun testGetVariantsMethodExists() {
        val method = DbtModelReferenceBase::class.java.methods.find { it.name == "getVariants" }
        assertNotNull("getVariants method should exist", method)
    }
}

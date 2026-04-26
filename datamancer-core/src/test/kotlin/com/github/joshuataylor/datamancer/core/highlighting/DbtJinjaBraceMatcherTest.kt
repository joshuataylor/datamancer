package com.github.joshuataylor.datamancer.core.highlighting

import com.intellij.jinja.lexer.Jinja2TokenTypes
import com.intellij.lang.PairedBraceMatcher
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for DbtJinjaBraceMatcher.
 * Tests brace matching for parentheses and square brackets in Jinja2 templates.
 */
class DbtJinjaBraceMatcherTest : BasePlatformTestCase() {
    private lateinit var braceMatcher: DbtJinjaBraceMatcher

    override fun setUp() {
        super.setUp()
        braceMatcher = DbtJinjaBraceMatcher()
    }

    // Basic instantiation tests
    fun testMatcherCanBeInstantiated() {
        assertNotNull(braceMatcher)
    }

    fun testMatcherIsPairedBraceMatcher() {
        assertTrue(braceMatcher is PairedBraceMatcher)
    }

    // Brace pairs tests
    fun testGetPairsReturnsNonEmpty() {
        val pairs = braceMatcher.pairs
        assertNotNull(pairs)
        assertTrue("Pairs should not be empty", pairs.isNotEmpty())
    }

    fun testGetPairsReturnsTwoPairs() {
        val pairs = braceMatcher.pairs
        assertEquals("Should have exactly 2 pairs (parens and brackets)", 2, pairs.size)
    }

    fun testBraceTokensIncludeParens() {
        val pairs = braceMatcher.pairs
        val parenPair = pairs.find {
            it.leftBraceType == Jinja2TokenTypes.LPAR && it.rightBraceType == Jinja2TokenTypes.RPAR
        }
        assertNotNull("Should have parentheses pair (LPAR, RPAR)", parenPair)
    }

    fun testBraceTokensIncludeBrackets() {
        val pairs = braceMatcher.pairs
        val bracketPair = pairs.find {
            it.leftBraceType == Jinja2TokenTypes.LBRACKET && it.rightBraceType == Jinja2TokenTypes.RBRACKET
        }
        assertNotNull("Should have bracket pair (LBRACKET, RBRACKET)", bracketPair)
    }

    fun testParenPairIsNotStructural() {
        val pairs = braceMatcher.pairs
        val parenPair = pairs.find {
            it.leftBraceType == Jinja2TokenTypes.LPAR
        }
        assertNotNull(parenPair)
        assertFalse("Paren pair should not be structural", parenPair!!.isStructural)
    }

    fun testBracketPairIsNotStructural() {
        val pairs = braceMatcher.pairs
        val bracketPair = pairs.find {
            it.leftBraceType == Jinja2TokenTypes.LBRACKET
        }
        assertNotNull(bracketPair)
        assertFalse("Bracket pair should not be structural", bracketPair!!.isStructural)
    }

    // isPairedBracesAllowedBeforeType tests
    fun testIsPairedBracesAllowedBeforeTypeReturnsTrue() {
        val result = braceMatcher.isPairedBracesAllowedBeforeType(
            Jinja2TokenTypes.LPAR,
            null
        )
        assertTrue("Should allow paired braces before any type", result)
    }

    fun testIsPairedBracesAllowedBeforeTypeWithContextReturnsTrue() {
        val result = braceMatcher.isPairedBracesAllowedBeforeType(
            Jinja2TokenTypes.LPAR,
            Jinja2TokenTypes.RPAR
        )
        assertTrue("Should allow paired braces with context type", result)
    }

    fun testIsPairedBracesAllowedForBrackets() {
        val result = braceMatcher.isPairedBracesAllowedBeforeType(
            Jinja2TokenTypes.LBRACKET,
            null
        )
        assertTrue("Should allow paired brackets", result)
    }

    // getCodeConstructStart tests
    fun testGetCodeConstructStartReturnsOffset() {
        val offset = 42
        val result = braceMatcher.getCodeConstructStart(null, offset)
        assertEquals("Should return the opening brace offset", offset, result)
    }

    fun testGetCodeConstructStartWithZeroOffset() {
        val result = braceMatcher.getCodeConstructStart(null, 0)
        assertEquals("Should return 0 for zero offset", 0, result)
    }

    // Multiple instance tests
    fun testMultipleInstancesCanBeCreated() {
        val matcher1 = DbtJinjaBraceMatcher()
        val matcher2 = DbtJinjaBraceMatcher()

        assertNotNull(matcher1)
        assertNotNull(matcher2)
        assertNotSame(matcher1, matcher2)
    }

    fun testMultipleInstancesReturnSamePairs() {
        val matcher1 = DbtJinjaBraceMatcher()
        val matcher2 = DbtJinjaBraceMatcher()

        assertEquals(matcher1.pairs.size, matcher2.pairs.size)
    }

    // Edge case tests
    fun testAllPairsHaveLeftAndRightBraces() {
        val pairs = braceMatcher.pairs
        for (pair in pairs) {
            assertNotNull("Left brace type should not be null", pair.leftBraceType)
            assertNotNull("Right brace type should not be null", pair.rightBraceType)
        }
    }

    fun testLeftAndRightBracesAreDifferent() {
        val pairs = braceMatcher.pairs
        for (pair in pairs) {
            assertNotSame(
                "Left and right brace types should be different",
                pair.leftBraceType,
                pair.rightBraceType
            )
        }
    }
}

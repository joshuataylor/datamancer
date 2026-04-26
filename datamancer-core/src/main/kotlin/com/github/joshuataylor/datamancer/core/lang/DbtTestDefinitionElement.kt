package com.github.joshuataylor.datamancer.core.lang

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiInvalidElementAccessException
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.impl.FakePsiElement
import com.intellij.util.IncorrectOperationException

/**
 * Synthetic PSI element representing a dbt generic test definition.
 *
 * Wraps the leaf PSI element at a `{percent} test name(...) {percent}` definition site,
 * implementing [PsiNamedElement] so that IntelliJ's Find Usages infrastructure can
 * identify the test by name and search for references to it.
 *
 * Navigation delegates to the actual leaf element in the file so that
 * Go-to-definition still lands at the correct position.
 *
 * @param testFile The PsiFile containing the test definition
 * @param testName The test name (e.g. "is_positive")
 * @param nameRange The text range of the test name within the file
 */
class DbtTestDefinitionElement(
    private val testFile: PsiFile,
    private val testName: String,
    private val nameRange: TextRange
) : FakePsiElement(), PsiNamedElement {

    override fun getParent(): PsiElement = testFile

    override fun getContainingFile(): PsiFile = testFile

    override fun getName(): String = testName

    override fun getTextRange(): TextRange = nameRange

    override fun getTextOffset(): Int = nameRange.startOffset

    override fun getTextLength(): Int = nameRange.length

    override fun getText(): String = testName

    override fun isValid(): Boolean = testFile.isValid

    @Throws(PsiInvalidElementAccessException::class)
    override fun getProject(): Project = testFile.project

    @Throws(IncorrectOperationException::class)
    override fun setName(name: String): PsiElement {
        throw IncorrectOperationException("Test rename not supported via this element")
    }

    override fun getNavigationElement(): PsiElement {
        return testFile.findElementAt(nameRange.startOffset) ?: this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DbtTestDefinitionElement) return false
        return testFile == other.testFile && testName == other.testName && nameRange == other.nameRange
    }

    override fun hashCode(): Int {
        var result = testFile.hashCode()
        result = 31 * result + testName.hashCode()
        result = 31 * result + nameRange.hashCode()
        return result
    }
}

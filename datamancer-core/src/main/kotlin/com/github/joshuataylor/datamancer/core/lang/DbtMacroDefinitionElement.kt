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
 * Synthetic PSI element representing a dbt macro definition.
 *
 * Wraps the leaf PSI element at a `{percent} macro name(...) {percent}` definition site,
 * implementing [PsiNamedElement] so that IntelliJ's Find Usages infrastructure can
 * identify the macro by name and search for references to it.
 *
 * Navigation delegates to the actual leaf element in the file so that
 * Go-to-definition still lands at the correct position.
 *
 * @param macroFile The PsiFile containing the macro definition
 * @param macroName The macro name (e.g. "max_changed_incremental")
 * @param nameRange The text range of the macro name within the file
 */
class DbtMacroDefinitionElement(
    private val macroFile: PsiFile,
    private val macroName: String,
    private val nameRange: TextRange
) : FakePsiElement(), PsiNamedElement {

    override fun getParent(): PsiElement = macroFile

    override fun getContainingFile(): PsiFile = macroFile

    override fun getName(): String = macroName

    override fun getTextRange(): TextRange = nameRange

    override fun getTextOffset(): Int = nameRange.startOffset

    override fun getTextLength(): Int = nameRange.length

    override fun getText(): String = macroName

    override fun isValid(): Boolean = macroFile.isValid

    @Throws(PsiInvalidElementAccessException::class)
    override fun getProject(): Project = macroFile.project

    @Throws(IncorrectOperationException::class)
    override fun setName(name: String): PsiElement {
        throw IncorrectOperationException("Macro rename not supported via this element")
    }

    override fun getNavigationElement(): PsiElement {
        return macroFile.findElementAt(nameRange.startOffset) ?: this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DbtMacroDefinitionElement) return false
        return macroFile == other.macroFile && macroName == other.macroName && nameRange == other.nameRange
    }

    override fun hashCode(): Int {
        var result = macroFile.hashCode()
        result = 31 * result + macroName.hashCode()
        result = 31 * result + nameRange.hashCode()
        return result
    }
}

package com.github.joshuataylor.datamancer.core.lang

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.jinja.Jinja2FileType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

/**
 * Typed handler for automatic quote completion in Jinja2 templates.
 *
 * When the user types a single or double quote character, this handler
 * automatically inserts the matching closing quote if one doesn't already exist.
 *
 * Example: Typing `'` will result in `'|'` with the caret between the quotes.
 */
class DbtJinjaTypedHandler : TypedHandlerDelegate() {

    override fun charTyped(c: Char, project: Project, editor: Editor, file: PsiFile): Result {
        // Only handle Jinja2 files
        if (file.fileType != Jinja2FileType.INSTANCE) {
            return Result.CONTINUE
        }

        // Only handle quote characters
        if (c != '"' && c != '\'') {
            return Result.CONTINUE
        }

        val caretOffset = editor.caretModel.offset

        // Check if we should insert a matching quote
        // Don't insert if the character 2 positions before is the same quote
        // (this prevents double-quoting when the user types a quote after an existing one)
        if (caretOffset > 1 && editor.document.charsSequence[caretOffset - 2] != c) {
            editor.document.insertString(caretOffset, c.toString())
        }

        return Result.CONTINUE
    }
}

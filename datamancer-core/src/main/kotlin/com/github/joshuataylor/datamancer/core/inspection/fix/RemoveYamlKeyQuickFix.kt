package com.github.joshuataylor.datamancer.core.inspection.fix

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import org.jetbrains.yaml.psi.YAMLKeyValue

/**
 * Quick-fix that removes a YAML key-value pair entirely.
 * Uses YAMLMapping.deleteKeyValue() for clean removal (handles excess newlines).
 */
class RemoveYamlKeyQuickFix(
    private val keyName: String,
    private val hint: String? = null,
) : LocalQuickFix {

    override fun getFamilyName(): String = "Remove deprecated dbt key"

    override fun getName(): String = buildString {
        append("Remove '$keyName'")
        if (hint != null) {
            append(" ($hint)")
        }
    }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement ?: return
        val keyValue = element.parent as? YAMLKeyValue ?: return
        val parentMapping = keyValue.parentMapping
        if (parentMapping != null) {
            parentMapping.deleteKeyValue(keyValue)
        } else {
            keyValue.delete()
        }
    }
}

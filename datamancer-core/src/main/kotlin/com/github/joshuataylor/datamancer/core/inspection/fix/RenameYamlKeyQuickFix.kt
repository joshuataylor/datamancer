package com.github.joshuataylor.datamancer.core.inspection.fix

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import org.jetbrains.yaml.psi.YAMLKeyValue

/**
 * Quick-fix that renames a YAML key while preserving its value.
 * Uses YAMLKeyValue.setName() which delegates to YAMLUtil.rename().
 */
class RenameYamlKeyQuickFix(
    private val oldKey: String,
    private val newKey: String,
) : LocalQuickFix {

    override fun getFamilyName(): String = "Rename dbt deprecated key"

    override fun getName(): String = "Rename '$oldKey' to '$newKey'"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement ?: return
        val keyValue = element.parent as? YAMLKeyValue ?: return
        keyValue.setName(newKey)
    }
}

package com.github.joshuataylor.datamancer.core.inspection.fix

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import org.jetbrains.yaml.psi.YAMLKeyValue

/**
 * Quick-fix that prepends '+' to a YAML key to mark it as a dbt config.
 * Uses YAMLKeyValue.setName() which delegates to YAMLUtil.rename().
 */
class AddPlusPrefixQuickFix(
    private val keyName: String,
) : LocalQuickFix {

    override fun getFamilyName(): String = "Add '+' prefix to dbt config key"

    override fun getName(): String = "Change '$keyName' to '+$keyName'"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement ?: return
        val keyValue = element.parent as? YAMLKeyValue ?: return
        keyValue.setName("+$keyName")
    }
}

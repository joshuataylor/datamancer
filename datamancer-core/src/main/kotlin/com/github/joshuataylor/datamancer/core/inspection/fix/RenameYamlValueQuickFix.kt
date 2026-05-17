package com.github.joshuataylor.datamancer.core.inspection.fix

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import org.jetbrains.yaml.YAMLElementGenerator
import org.jetbrains.yaml.psi.YAMLScalar

/**
 * Quick-fix that replaces a YAML scalar value with a new value.
 */
class RenameYamlValueQuickFix(
    private val oldValue: String,
    private val newValue: String,
) : LocalQuickFix {

    override fun getFamilyName(): String = "Replace deprecated dbt value"

    override fun getName(): String = "Replace '$oldValue' with '$newValue'"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement as? YAMLScalar ?: return
        val generator = YAMLElementGenerator.getInstance(project)
        val newScalar = generator.createYamlKeyValue("k", newValue).value ?: return
        element.replace(newScalar)
    }
}

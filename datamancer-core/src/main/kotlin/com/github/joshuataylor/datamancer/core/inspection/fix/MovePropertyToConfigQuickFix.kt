package com.github.joshuataylor.datamancer.core.inspection.fix

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import org.jetbrains.yaml.YAMLElementGenerator
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping

/**
 * Quick-fix that moves a top-level property under the `config:` mapping.
 * Creates the `config:` key if it does not already exist.
 */
class MovePropertyToConfigQuickFix(
    private val propertyName: String,
) : LocalQuickFix {

    override fun getFamilyName(): String = "Move property under 'config'"

    override fun getName(): String = "Move '$propertyName' under 'config'"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement ?: return
        val keyValue = element.parent as? YAMLKeyValue ?: return
        val parentMapping = keyValue.parentMapping ?: return
        val generator = YAMLElementGenerator.getInstance(project)

        val configKeyValue = parentMapping.getKeyValueByKey("config")

        if (configKeyValue != null) {
            // config: already exists -- add the property to its mapping
            val configMapping = configKeyValue.value as? YAMLMapping
            if (configMapping != null) {
                val newKv = generator.createYamlKeyValue(propertyName, keyValue.valueText)
                configMapping.putKeyValue(newKv)
            } else {
                // config: exists but has no mapping value (unlikely but handle gracefully)
                val newValue = generator.createYamlKeyValue("config", "$propertyName: ${keyValue.valueText}")
                configKeyValue.setValue(newValue.value!!)
            }
        } else {
            // No config: key yet -- create one with the property inside
            val configKv = generator.createYamlKeyValue("config", "$propertyName: ${keyValue.valueText}")
            parentMapping.putKeyValue(configKv)
        }

        // Remove the original property from its current location
        parentMapping.deleteKeyValue(keyValue)
    }
}

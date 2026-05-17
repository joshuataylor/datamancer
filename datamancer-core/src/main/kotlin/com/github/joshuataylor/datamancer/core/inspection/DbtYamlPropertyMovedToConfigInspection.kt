package com.github.joshuataylor.datamancer.core.inspection

import com.github.joshuataylor.datamancer.core.inspection.fix.MovePropertyToConfigQuickFix
import com.intellij.codeInspection.ProblemsHolder
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLSequence
import org.jetbrains.yaml.psi.YAMLSequenceItem
import org.jetbrains.yaml.psi.YamlPsiElementVisitor

/**
 * Reports properties that should be moved under `config:` in dbt schema YAML files.
 *
 * Since dbt v1.10, the following properties on resource definitions are being moved
 * to configs: `freshness`, `meta`, `tags`, `docs`, `group`, `access`.
 * Using them as top-level properties raises PropertyMovedToConfigDeprecation.
 */
class DbtYamlPropertyMovedToConfigInspection : DbtDeprecationInspectionBase() {

    override fun getStaticDescription(): String = """
        Reports resource properties that should be nested under 'config:' in dbt YAML files.
        <p>
        Since dbt v1.10, properties like <code>meta</code>, <code>tags</code>, <code>docs</code>,
        <code>group</code>, <code>access</code>, and <code>freshness</code> must be placed under
        <code>config:</code> on resource definitions. A quick-fix is available to move them automatically.
        </p>
    """.trimIndent()

    override fun isApplicableFile(file: YAMLFile): Boolean {
        // Apply to schema YAML files but NOT dbt_project.yml
        return !isDbtProjectYml(file)
    }

    override fun buildYamlVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): YamlPsiElementVisitor {
        return object : YamlPsiElementVisitor() {

            override fun visitKeyValue(keyValue: YAMLKeyValue) {
                val key = keyValue.keyText
                if (key !in MOVED_PROPERTIES) return

                // Check we're on a resource definition (direct child of a resource list item)
                val context = getResourceContext(keyValue) ?: return

                // Don't flag if already under config:
                if (isUnderConfigKey(keyValue)) return

                // Check the property is valid for this resource type
                if (!isValidForResourceType(key, context)) return

                val keyElement = keyValue.key ?: return
                holder.registerProblem(
                    keyElement,
                    "'$key' should be nested under 'config:' (PropertyMovedToConfigDeprecation)",
                    MovePropertyToConfigQuickFix(key),
                )
            }
        }
    }

    /**
     * Determines the resource type context by walking up the PSI tree.
     * Returns the node-type key name (e.g. "models", "sources") or null.
     */
    private fun getResourceContext(keyValue: YAMLKeyValue): String? {
        // Expected structure: key -> YAMLMapping -> YAMLSequenceItem -> YAMLSequence -> YAMLKeyValue(node-type)
        val mapping = keyValue.parent as? YAMLMapping ?: return null
        val seqItem = mapping.parent as? YAMLSequenceItem ?: return null
        val sequence = seqItem.parent as? YAMLSequence ?: return null
        val nodeTypeKv = sequence.parent as? YAMLKeyValue ?: return null
        val nodeType = nodeTypeKv.keyText

        // Also handle source tables: sources -> [item] -> tables -> [item]
        if (nodeType == "tables") {
            // Walk up one more level to confirm we're inside sources
            val parentMapping = nodeTypeKv.parent as? YAMLMapping ?: return null
            val parentSeqItem = parentMapping.parent as? YAMLSequenceItem ?: return null
            val parentSeq = parentSeqItem.parent as? YAMLSequence ?: return null
            val grandparentKv = parentSeq.parent as? YAMLKeyValue ?: return null
            if (grandparentKv.keyText == "sources") return "sources"
        }

        return if (nodeType in RESOURCE_TYPES) nodeType else null
    }

    private fun isUnderConfigKey(keyValue: YAMLKeyValue): Boolean {
        val parentMapping = keyValue.parent as? YAMLMapping ?: return false
        val parentKv = parentMapping.parent as? YAMLKeyValue ?: return false
        return parentKv.keyText == "config"
    }

    private fun isValidForResourceType(property: String, resourceType: String): Boolean {
        return when (property) {
            "freshness" -> resourceType == "sources"
            "access" -> resourceType == "models"
            "group" -> resourceType in setOf("models", "seeds", "snapshots")
            else -> true // meta, tags, docs apply to all resource types
        }
    }

    companion object {
        private val MOVED_PROPERTIES = setOf("freshness", "meta", "tags", "docs", "group", "access")
        private val RESOURCE_TYPES = setOf("models", "seeds", "sources", "snapshots", "tests", "analyses")
    }
}

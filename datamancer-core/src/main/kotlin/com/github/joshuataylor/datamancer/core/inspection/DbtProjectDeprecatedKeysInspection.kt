package com.github.joshuataylor.datamancer.core.inspection

import com.github.joshuataylor.datamancer.core.inspection.fix.RemoveYamlKeyQuickFix
import com.github.joshuataylor.datamancer.core.inspection.fix.RenameYamlKeyQuickFix
import com.github.joshuataylor.datamancer.core.inspection.fix.RenameYamlValueQuickFix
import com.intellij.codeInspection.ProblemsHolder
import org.jetbrains.yaml.psi.YAMLDocument
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.YAMLSequence
import org.jetbrains.yaml.psi.YamlPsiElementVisitor

/**
 * Reports deprecated keys in dbt_project.yml that were renamed or removed in dbt v1.0+.
 *
 * Detected deprecations:
 * - `data-paths` renamed to `seed-paths` (since v1.0)
 * - `source-paths` renamed to `model-paths` (since v1.0)
 * - `target-path` removed (since v1.5, use --target-path CLI flag or DBT_ENGINE_TARGET_PATH)
 * - `log-path` removed (since v1.5, use --log-path CLI flag or DBT_ENGINE_LOG_PATH)
 * - `dbt_modules` in `clean-targets` should be `dbt_packages`
 */
class DbtProjectDeprecatedKeysInspection : DbtDeprecationInspectionBase() {

    override fun getStaticDescription(): String = """
        Reports deprecated configuration keys in dbt_project.yml.
        <p>
        These keys have been renamed or removed in recent dbt versions and will
        cause errors in dbt Fusion. Quick-fixes are available to update them automatically.
        </p>
    """.trimIndent()

    override fun isApplicableFile(file: YAMLFile): Boolean = isDbtProjectYml(file)

    override fun buildYamlVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): YamlPsiElementVisitor {
        return object : YamlPsiElementVisitor() {

            override fun visitKeyValue(keyValue: YAMLKeyValue) {
                // Only check top-level keys (direct children of the document root mapping)
                if (!isTopLevelKey(keyValue)) return

                when (keyValue.keyText) {
                    "data-paths" -> reportRenamedKey(holder, keyValue, "data-paths", "seed-paths")
                    "source-paths" -> reportRenamedKey(holder, keyValue, "source-paths", "model-paths")
                    "target-path" -> reportRemovedKey(
                        holder, keyValue, "target-path",
                        "use --target-path CLI flag or DBT_ENGINE_TARGET_PATH env var"
                    )
                    "log-path" -> reportRemovedKey(
                        holder, keyValue, "log-path",
                        "use --log-path CLI flag or DBT_ENGINE_LOG_PATH env var"
                    )
                    "clean-targets" -> checkCleanTargets(holder, keyValue)
                }
            }
        }
    }

    private fun isTopLevelKey(keyValue: YAMLKeyValue): Boolean {
        val parent = keyValue.parent as? YAMLMapping ?: return false
        return parent.parent is YAMLDocument
    }

    private fun reportRenamedKey(
        holder: ProblemsHolder,
        keyValue: YAMLKeyValue,
        oldKey: String,
        newKey: String,
    ) {
        val keyElement = keyValue.key ?: return
        holder.registerProblem(
            keyElement,
            "'$oldKey' has been renamed to '$newKey'",
            RenameYamlKeyQuickFix(oldKey, newKey),
        )
    }

    private fun reportRemovedKey(
        holder: ProblemsHolder,
        keyValue: YAMLKeyValue,
        keyName: String,
        hint: String,
    ) {
        val keyElement = keyValue.key ?: return
        holder.registerProblem(
            keyElement,
            "'$keyName' is deprecated ($hint)",
            RemoveYamlKeyQuickFix(keyName, hint),
        )
    }

    private fun checkCleanTargets(holder: ProblemsHolder, keyValue: YAMLKeyValue) {
        val sequence = keyValue.value as? YAMLSequence ?: return
        for (item in sequence.items) {
            val scalar = item.value as? YAMLScalar ?: continue
            if (scalar.textValue == "dbt_modules") {
                holder.registerProblem(
                    scalar,
                    "'dbt_modules' should be replaced with 'dbt_packages'",
                    RenameYamlValueQuickFix("dbt_modules", "dbt_packages"),
                )
            }
        }
    }
}

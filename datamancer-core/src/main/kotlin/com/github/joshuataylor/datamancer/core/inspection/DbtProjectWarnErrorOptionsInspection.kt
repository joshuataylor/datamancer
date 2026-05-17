package com.github.joshuataylor.datamancer.core.inspection

import com.github.joshuataylor.datamancer.core.inspection.fix.RenameYamlKeyQuickFix
import com.intellij.codeInspection.ProblemsHolder
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YamlPsiElementVisitor

/**
 * Reports deprecated `include`/`exclude` keys under `flags.warn_error_options` in dbt_project.yml.
 *
 * Since dbt v1.10, these have been renamed:
 * - `include` -> `error`
 * - `exclude` -> `warn`
 */
class DbtProjectWarnErrorOptionsInspection : DbtDeprecationInspectionBase() {

    override fun getStaticDescription(): String = """
        Reports deprecated 'include' and 'exclude' options under 'flags.warn_error_options' in dbt_project.yml.
        <p>
        Since dbt v1.10, <code>include</code> has been renamed to <code>error</code> and
        <code>exclude</code> has been renamed to <code>warn</code>.
        </p>
    """.trimIndent()

    override fun isApplicableFile(file: YAMLFile): Boolean = isDbtProjectYml(file)

    override fun buildYamlVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): YamlPsiElementVisitor {
        return object : YamlPsiElementVisitor() {

            override fun visitKeyValue(keyValue: YAMLKeyValue) {
                val key = keyValue.keyText
                if (key != "include" && key != "exclude") return
                if (!isUnderWarnErrorOptions(keyValue)) return

                val keyElement = keyValue.key ?: return
                when (key) {
                    "include" -> holder.registerProblem(
                        keyElement,
                        "'include' has been renamed to 'error' in warn_error_options",
                        RenameYamlKeyQuickFix("include", "error"),
                    )
                    "exclude" -> holder.registerProblem(
                        keyElement,
                        "'exclude' has been renamed to 'warn' in warn_error_options",
                        RenameYamlKeyQuickFix("exclude", "warn"),
                    )
                }
            }
        }
    }

    /**
     * Checks if the key-value is nested under flags.warn_error_options.
     */
    private fun isUnderWarnErrorOptions(keyValue: YAMLKeyValue): Boolean {
        val parentMapping = keyValue.parent as? YAMLMapping ?: return false
        val warnErrorKv = parentMapping.parent as? YAMLKeyValue ?: return false
        if (warnErrorKv.keyText != "warn_error_options") return false

        val flagsMapping = warnErrorKv.parent as? YAMLMapping ?: return false
        val flagsKv = flagsMapping.parent as? YAMLKeyValue ?: return false
        return flagsKv.keyText == "flags"
    }
}

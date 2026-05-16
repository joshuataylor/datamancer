package com.github.joshuataylor.datamancer.core.lang

import com.github.joshuataylor.datamancer.core.DatamancerUtils
import com.intellij.jinja.tags.Jinja2FunctionCall
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.ResolveResult
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.yaml.psi.YAMLScalar

/**
 * Reference from a dbt source or table name in a YAML schema file to the
 * SQL model files that use that source via `{{ source() }}` calls.
 *
 * When [tableName] is null, matches any `source('sourceName', ...)` call.
 * When [tableName] is set, matches only `source('sourceName', 'tableName')`.
 *
 * If multiple SQL files reference the same source, IntelliJ shows a chooser popup.
 *
 * @param element The YAMLScalar containing the source or table name
 * @param textRange Text range within the element
 * @param sourceName The source name to match
 * @param tableName The table name to match, or null to match any table in the source
 */
class DbtYamlSourceUsageReference(
    element: YAMLScalar,
    textRange: TextRange,
    private val sourceName: String,
    private val tableName: String?
) : DbtModelReferenceBase<YAMLScalar>(element, textRange, tableName ?: sourceName) {

    @RequiresReadLock
    override fun resolveInner(incompleteCode: Boolean): Array<ResolveResult> {
        val project = element.project
        val allModels = DbtDirectories.findAllModels(project)
        val results = mutableListOf<ResolveResult>()

        for (modelFile in allModels) {
            ProgressManager.checkCanceled()
            val sourceCalls = PsiTreeUtil.collectElementsOfType(modelFile, Jinja2FunctionCall::class.java)

            for (call in sourceCalls) {
                ProgressManager.checkCanceled()
                if (!DatamancerUtils.isSourceCall(call)) continue

                val args = DatamancerUtils.getSourceArguments(call) ?: continue
                if (args.first != sourceName) continue

                if (tableName == null || args.second == tableName) {
                    results.add(PsiElementResolveResult(modelFile))
                    break // one match per file is enough
                }
            }
        }

        return results.toTypedArray()
    }
}

package com.github.joshuataylor.datamancer.core.lang

import com.github.joshuataylor.datamancer.core.services.DatamancerDbtProjectIndexService
import com.github.joshuataylor.datamancer.core.services.DbtColumnMetadataService
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.module.ModuleUtil
import com.intellij.psi.PsiElement
import javax.swing.Icon

/**
 * Gutter icon provider for navigating from a dbt SQL model file to its
 * YAML schema definition.
 *
 * Shows a gutter icon on the first line of `.sql` files inside a dbt
 * project's `models/` directory when a matching entry exists in a YAML
 * schema file (e.g. `schema.yml` with `models: - name: <model_name>`).
 */
class DatamancerYamlDefinitionLineMarkerProvider : RelatedItemLineMarkerProvider() {

    override fun getName(): String = "dbt YAML definition"

    override fun getIcon(): Icon = AllIcons.Gutter.ImplementedMethod

    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        if (!isFirstLeafElement(element)) return

        val containingFile = element.containingFile ?: return
        val virtualFile = containingFile.virtualFile ?: return

        if (!virtualFile.name.endsWith(".sql")) return
        if (!virtualFile.path.contains("/models/")) return

        val project = element.project
        val module = ModuleUtil.findModuleForFile(virtualFile, project) ?: return
        val indexService = DatamancerDbtProjectIndexService.getInstance(project)
        indexService.getDbtConfigForModuleName(module.name) ?: return

        val modelName = virtualFile.nameWithoutExtension
        val metadata = DbtColumnMetadataService.getInstance(project).findModel(modelName) ?: return

        val builder = NavigationGutterIconBuilder.create(AllIcons.Gutter.ImplementedMethod)
            .setTargets(listOf(metadata.psiElement))
            .setTooltipText("Navigate to YAML definition")

        result.add(builder.createLineMarkerInfo(element))
    }

    /**
     * Finds the first meaningful (non-blank, non-comment) leaf element in
     * the file and returns whether [element] is that element.
     *
     * Mirrors the logic in [DatamancerRunLineMarkerContributor] to ensure
     * only one gutter icon appears per file.
     */
    private fun isFirstLeafElement(element: PsiElement): Boolean {
        val file = element.containingFile ?: return false

        var first: PsiElement? = file.firstChild
        while (first != null && first.firstChild != null) {
            first = first.firstChild
        }

        while (first != null && (first.text.isBlank() || isComment(first))) {
            first = first.nextSibling
            while (first != null && first.firstChild != null) {
                first = first.firstChild
            }
        }

        return element == first
    }

    private fun isComment(element: PsiElement): Boolean {
        val elementType = element.node?.elementType?.toString() ?: return false
        return elementType.contains("COMMENT", ignoreCase = true)
    }
}

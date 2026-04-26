package com.github.joshuataylor.datamancer.core.lang

import com.github.joshuataylor.datamancer.core.services.DbtGenericTestIndexService
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiManager
import com.intellij.psi.ResolveResult
import org.jetbrains.yaml.psi.YAMLScalar

/**
 * Reference from a generic test name in a YAML schema file to its definition
 * in a .sql file.
 *
 * Resolves `is_positive` under `data_tests:` to `tests/generic/test_is_positive.sql`
 * (or wherever the `{percent} test is_positive(...) {percent}` block is defined).
 *
 * @param element The YAMLScalar containing the test name
 * @param textRange Text range of the test name within the element
 * @param testName The test name to resolve
 */
class DbtYamlGenericTestReference(
    element: YAMLScalar,
    textRange: TextRange,
    testName: String
) : DbtModelReferenceBase<YAMLScalar>(element, textRange, testName) {

    override fun resolveInner(incompleteCode: Boolean): Array<ResolveResult> {
        val project = element.project
        val testIndexService = DbtGenericTestIndexService.getInstance(project)
        val testDef = testIndexService.findGenericTest(modelName)
            ?: return ResolveResult.EMPTY_ARRAY

        val psiManager = PsiManager.getInstance(project)
        val psiFile = psiManager.findFile(testDef.file)
            ?: return ResolveResult.EMPTY_ARRAY

        val nameRange = TextRange(testDef.textOffset, testDef.textOffset + testDef.nameLength)
        val target = DbtTestDefinitionElement(psiFile, testDef.name, nameRange)

        return arrayOf(PsiElementResolveResult(target))
    }
}

package com.github.joshuataylor.datamancer.core.lang

import com.github.joshuataylor.datamancer.core.services.DatamancerDbtProjectIndexService
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.YAMLSequence
import org.jetbrains.yaml.psi.YAMLSequenceItem

/**
 * Reference provider for dbt generic test names in YAML schema files.
 *
 * Detects test name references under `data_tests:` (or legacy `tests:`) sequences
 * and creates references for go-to-definition navigation to the test's SQL definition.
 *
 * Handles two YAML forms:
 *
 * Form 1 - plain scalar (no arguments):
 * ```yaml
 * data_tests:
 *   - is_positive
 * ```
 *
 * Form 2 - mapping key (with arguments):
 * ```yaml
 * data_tests:
 *   - accepted_range:
 *       arguments:
 *         min_value: 0
 * ```
 */
class DbtYamlDataTestReferenceProvider : PsiReferenceProvider() {

    companion object {
        private val DATA_TEST_KEYS = setOf("data_tests", "tests")
    }

    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<PsiReference> {
        val scalar = element as? YAMLScalar ?: return PsiReference.EMPTY_ARRAY

        // Must be in a dbt project
        val project = element.project
        val indexService = DatamancerDbtProjectIndexService.getInstance(project)
        if (indexService.getAllDbtConfigsSync().isEmpty()) {
            return PsiReference.EMPTY_ARRAY
        }

        val testName = getDataTestName(scalar) ?: return PsiReference.EMPTY_ARRAY
        if (testName.isBlank()) return PsiReference.EMPTY_ARRAY

        val textRange = ElementManipulators.getValueTextRange(scalar)
        return arrayOf(DbtYamlGenericTestReference(scalar, textRange, testName))
    }

    /**
     * Extracts the test name if this scalar is a data_tests entry.
     *
     * @return The test name, or null if this scalar is not in a data_tests context
     */
    private fun getDataTestName(scalar: YAMLScalar): String? {
        val parent = scalar.parent

        // Form 1: Plain scalar test item
        // YAMLScalar -> YAMLSequenceItem -> YAMLSequence -> YAMLKeyValue(data_tests)
        if (parent is YAMLSequenceItem) {
            val parentKey = getSequenceParentKeyText(parent) ?: return null
            if (parentKey in DATA_TEST_KEYS) {
                return scalar.textValue
            }
        }

        // Form 2: Mapping key test item
        // YAMLScalar (key) -> YAMLKeyValue -> YAMLMapping -> YAMLSequenceItem -> YAMLSequence -> YAMLKeyValue(data_tests)
        if (parent is YAMLKeyValue && parent.key == scalar) {
            val mapping = parent.parent as? YAMLMapping ?: return null
            val seqItem = mapping.parent as? YAMLSequenceItem ?: return null
            val parentKey = getSequenceParentKeyText(seqItem) ?: return null
            if (parentKey in DATA_TEST_KEYS) {
                return scalar.textValue
            }
        }

        return null
    }

    /**
     * Gets the key text of the YAMLKeyValue that owns the sequence containing
     * the given sequence item.
     *
     * Traversal: YAMLSequenceItem -> YAMLSequence -> YAMLKeyValue
     */
    private fun getSequenceParentKeyText(seqItem: YAMLSequenceItem): String? {
        val sequence = seqItem.parent as? YAMLSequence ?: return null
        val keyValue = sequence.parent as? YAMLKeyValue ?: return null
        return keyValue.keyText
    }
}

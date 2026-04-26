package com.github.joshuataylor.datamancer.core.lang

import com.intellij.psi.ElementDescriptionLocation
import com.intellij.psi.ElementDescriptionProvider
import com.intellij.psi.PsiElement
import com.intellij.usageView.UsageViewLongNameLocation
import com.intellij.usageView.UsageViewShortNameLocation

/**
 * Provides display text for [DbtMacroDefinitionElement] and [DbtTestDefinitionElement]
 * in the Find Usages results pane.
 */
class DatamancerElementDescriptionProvider : ElementDescriptionProvider {

    override fun getElementDescription(element: PsiElement, location: ElementDescriptionLocation): String? {
        return when (element) {
            is DbtMacroDefinitionElement -> when (location) {
                is UsageViewLongNameLocation -> "macro ${element.name}"
                is UsageViewShortNameLocation -> element.name
                else -> null
            }
            is DbtTestDefinitionElement -> when (location) {
                is UsageViewLongNameLocation -> "test ${element.name}"
                is UsageViewShortNameLocation -> element.name
                else -> null
            }
            else -> null
        }
    }
}

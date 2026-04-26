package com.github.joshuataylor.datamancer.core

import com.intellij.openapi.util.IconLoader

/**
 * [Icons] class that holds icon resources.
 */
object Icons {
    /** General ignore icon. */
    @JvmField
    val KILN_LOGO = IconLoader.getIcon("/icons/icon.png", Icons::class.java)

    /** dbt project root directory icon. */
    @JvmField
    val PROJECT_ROOT = IconLoader.getIcon("/icons/dbtProjectRoot.svg", Icons::class.java)
}

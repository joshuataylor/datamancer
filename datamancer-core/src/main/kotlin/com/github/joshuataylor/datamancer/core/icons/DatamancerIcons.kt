package com.github.joshuataylor.datamancer.core.icons

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

/**
 * Icon holder for Datamancer plugin icons.
 *
 * Note: For now, this uses a test icon. The final release will use properly designed icons.
 */
object DatamancerIcons {
    /**
     * Icon for dbt SQL files (SQL files within dbt projects).
     * This is a test icon and will be replaced with a proper design for the final release.
     */
    @JvmField
    val DbtSqlFile: Icon = IconLoader.getIcon("/icons/dbtSqlFile.svg", javaClass)
}

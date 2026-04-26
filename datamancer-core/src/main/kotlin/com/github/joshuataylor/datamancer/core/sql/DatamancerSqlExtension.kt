package com.github.joshuataylor.datamancer.core.sql

import com.github.joshuataylor.datamancer.core.DatamancerUtils
import com.github.joshuataylor.datamancer.core.lang.DbtDirectories
import com.intellij.database.Dbms
import com.intellij.database.model.DasObject
import com.intellij.database.model.DasTypeAwareObject
import com.intellij.database.model.DataType
import com.intellij.database.model.ObjectKind
import com.intellij.database.symbols.DasSymbol
import com.intellij.database.types.DasBuiltinType
import com.intellij.database.types.DasType
import com.intellij.database.types.DasTypeSystemBase
import com.intellij.database.util.DbImplUtil
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.ResolveState
import com.intellij.psi.impl.FakePsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.intellij.sql.dialects.ReservedEntity.Typed
import com.intellij.sql.psi.*
import com.intellij.sql.psi.impl.SqlResolveExtension
import com.intellij.sql.psi.impl.SqlTableTypeBase
import com.intellij.sql.psi.impl.SqlTypeFactory
import com.intellij.util.containers.JBIterable

/**
 * SQL resolve extension for dbt model references.
 *
 * This extension integrates dbt `ref()` calls with the SQL language support,
 * enabling:
 * - Column completion from referenced models
 * - Go-to-definition from SQL table references
 * - Type information for CSV seed files
 */
class DatamancerSqlExtension : SqlResolveExtension {
    override fun process(reference: SqlReference, processor: SqlScopeProcessor): Boolean {
        val element = reference.element

        // Only handle SQL table references
        if (element.elementType != SqlCompositeElementTypes.SQL_TABLE_REFERENCE) {
            return true
        }

        // Find the parent query expression
        val queryExpression = PsiTreeUtil.getParentOfType(element, SqlQueryExpression::class.java)
            ?: return true

        // Get the FROM clause expression
        val tableExpression = queryExpression.tableExpression ?: return true
        val fromClause = tableExpression.fromClause ?: return true
        val fromExpression = fromClause.fromExpression ?: return true

        // Extract the Jinja2 function call from the SQL expression
        val jinjaCall = DatamancerUtils.getJinjaCall(fromExpression) ?: return true

        // Check if it's a ref() call
        if (!DatamancerUtils.isRefCall(jinjaCall)) {
            return true
        }

        // Get the referenced model name
        val modelName = DatamancerUtils.getReferencedName(jinjaCall) ?: return true

        val project = element.project

        // Try to find the model file first
        val modelPsiFile = DbtDirectories.findModel(project, modelName)

        if (modelPsiFile == null) {
            // Model not found, try to find a seed file
            val seedFile = DbtDirectories.findSeedFile(project, modelName) ?: return true
            val seedPsiFile = com.intellij.psi.PsiManager.getInstance(project).findFile(seedFile)
                ?: return true

            // Create seed type and symbol
            val seedWrapper = DatamancerSeedFile(seedPsiFile)
            val csvType = DatamancerCsvType(seedPsiFile)
            val typed = Typed(DbImplUtil.getDbms(element), element.text, ObjectKind.TABLE, csvType)
            val symbol = DatamancerModelDasSymbol(seedWrapper, element.text, typed)

            return processor.executeTarget(symbol, csvType, null, ResolveState.initial())
        }

        // Find the SQL language in the model file
        val sqlLanguage = modelPsiFile.viewProvider.languages
            .firstOrNull { it.isKindOf(SqlLanguage.INSTANCE) }
            ?: return true

        // Get the PSI for the SQL language
        val sqlPsiFile = modelPsiFile.viewProvider.getPsi(sqlLanguage) ?: return true

        // Find the last SELECT query in the model
        val lastQuery = DatamancerUtils.findLastSelectQuery(sqlPsiFile) ?: return true

        // Create the model symbol
        val symbol = DatamancerModelDasSymbol(lastQuery, element.text, null)

        return processor.executeTarget(symbol, null, null, ResolveState.initial())
    }

    /**
     * Represents a column from a CSV seed file.
     */
    inner class CsvColumnElement(
        private val columnName: String,
        private val parentFile: PsiFile,
        private val columnOffset: Int
    ) : FakePsiElement(), DasSymbol {

        override fun getParent(): PsiFile = parentFile

        override fun getName(): String = columnName

        override fun getKind(): ObjectKind = ObjectKind.COLUMN

        override fun isQuoted(): Boolean = false

        override fun getDbms(): Dbms = DbImplUtil.getDbms(this)

        override fun isValid(): Boolean = true

        override fun getDasObject(): DasObject? = null

        override fun getPsiDeclarations(): JBIterable<CsvColumnElement> = JBIterable.of(this)

        override fun getNavigationElement(): CsvColumnElement = this

        override fun getContextElement(): CsvColumnElement = this

        override fun getTextOffset(): Int = columnOffset

        override fun getText(): String = columnName
    }

    /**
     * Provides column type information for CSV seed files.
     *
     * Parses CSV to extract column names, handling quoted values correctly.
     */
    inner class DatamancerCsvType(file: PsiFile) : SqlTableTypeBase() {
        private val columnNames = mutableListOf<String>()
        private val columnOffsets = mutableListOf<Int>()
        private val columns = mutableListOf<PsiElement>()

        init {
            val text = file.text
            parseHeaderRow(text, file)
        }

        /**
         * Parses the header row of a CSV, handling quoted values correctly.
         */
        private fun parseHeaderRow(text: String, file: PsiFile) {
            var currentOffset = 0
            var cellStart = 0
            var inQuotes = false
            var wasQuoted = false
            val cellBuilder = StringBuilder()

            while (currentOffset < text.length) {
                val c = text[currentOffset]

                when {
                    c == '"' && !inQuotes -> {
                        inQuotes = true
                        wasQuoted = true
                        cellStart = currentOffset + 1
                    }

                    c == '"' && inQuotes -> {
                        // Check for escaped quote ("")
                        if (currentOffset + 1 < text.length && text[currentOffset + 1] == '"') {
                            cellBuilder.append('"')
                            currentOffset++
                        } else {
                            inQuotes = false
                        }
                    }

                    c == ',' && !inQuotes -> {
                        val value = if (wasQuoted) {
                            val result = cellBuilder.toString()
                            cellBuilder.clear()
                            result
                        } else {
                            text.substring(cellStart, currentOffset).trim()
                        }
                        addColumn(value, cellStart, file)
                        cellStart = currentOffset + 1
                        wasQuoted = false
                    }

                    c == '\n' && !inQuotes -> {
                        val value = if (wasQuoted) {
                            cellBuilder.toString()
                        } else {
                            text.substring(cellStart, currentOffset).trim()
                        }
                        if (value.isNotEmpty() || columnNames.isNotEmpty()) {
                            addColumn(value, cellStart, file)
                        }
                        return
                    }

                    c == '\r' && !inQuotes -> {
                        // Skip carriage return
                    }

                    inQuotes -> {
                        cellBuilder.append(c)
                    }
                }
                currentOffset++
            }

            // Handle last cell if no trailing newline
            if (cellStart < text.length || cellBuilder.isNotEmpty() || wasQuoted) {
                val value = if (wasQuoted) {
                    cellBuilder.toString()
                } else {
                    text.substring(cellStart).trim()
                }
                if (value.isNotEmpty() || columnNames.isNotEmpty()) {
                    addColumn(value, cellStart, file)
                }
            }
        }

        private fun addColumn(name: String, offset: Int, file: PsiFile) {
            columnNames.add(name)
            columnOffsets.add(offset)
            columns.add(CsvColumnElement(name, file, offset))
        }

        override fun toDataType(): DataType = SqlTypeFactory.createTableDataType("")

        override fun getColumnCount(): Int = columns.size

        override fun getColumnName(i: Int): String = columnNames[i]

        override fun getColumnDasType(i: Int): DasBuiltinType<*> = DasTypeSystemBase.UNKNOWN

        override fun getMethods(): MutableList<DasObject> = mutableListOf()

        override fun isColumnQuoted(i: Int): Boolean = false

        override fun getColumnElement(i: Int): PsiElement = columns[i]

        override fun getSourceColumnElement(i: Int): PsiElement? = null

        override fun getColumnQualifier(i: Int): PsiElement? = null
    }

    /**
     * Represents a dbt model or seed as a database symbol.
     */
    class DatamancerModelDasSymbol(
        private val element: PsiElement,
        private val symbolName: String,
        private val dasObject: DasObject?
    ) : UserDataHolderBase(), DasSymbol {

        override fun getName(): String = symbolName

        override fun getKind(): ObjectKind = ObjectKind.TABLE

        override fun isQuoted(): Boolean = false

        override fun getDbms(): Dbms = DbImplUtil.getDbms(element)

        override fun isValid(): Boolean = true

        override fun getDasObject(): DasObject? = dasObject

        override fun getPsiDeclarations(): JBIterable<PsiElement> = JBIterable.of(element)

        override fun getNavigationElement(): PsiElement = element

        override fun getContextElement(): PsiElement = element
    }

    /**
     * Wraps a CSV seed file as a typed PSI element.
     */
    inner class DatamancerSeedFile(
        private val originalFile: PsiFile
    ) : FakePsiElement(), DasTypeAwareObject {

        override fun getParent(): PsiFile = originalFile

        override fun getDasType(): DasType = DatamancerCsvType(originalFile)
    }
}

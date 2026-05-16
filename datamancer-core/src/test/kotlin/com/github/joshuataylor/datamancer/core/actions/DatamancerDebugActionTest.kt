package com.github.joshuataylor.datamancer.core.actions

import com.intellij.openapi.actionSystem.*
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for DatamancerDebugAction and DatamancerDebugInfoCollector.
 */
class DatamancerDebugActionTest : BasePlatformTestCase() {

    // ==================== DatamancerDebugAction Tests ====================

    fun testActionCanBeInstantiated() {
        val action = DatamancerDebugAction()
        assertNotNull(action)
    }

    fun testActionIsAnAction() {
        val action = DatamancerDebugAction()
        assertTrue(action is AnAction)
    }

    fun testActionUpdateThreadIsBGT() {
        val action = DatamancerDebugAction()
        assertEquals(ActionUpdateThread.BGT, action.actionUpdateThread)
    }

    fun testActionIsEnabledWhenProjectIsOpen() {
        val action = DatamancerDebugAction()
        val presentation = Presentation()
        val dataContext = createDataContextWithProject()
        val event = AnActionEvent.createFromDataContext("test", presentation, dataContext)

        action.update(event)

        assertTrue("Action should be enabled when project is open", presentation.isEnabled)
        assertTrue("Action should be visible when project is open", presentation.isVisible)
    }

    fun testActionIsDisabledWhenNoProject() {
        val action = DatamancerDebugAction()
        val presentation = Presentation()
        val dataContext = DataContext { null }
        val event = AnActionEvent.createFromDataContext("test", presentation, dataContext)

        action.update(event)

        assertFalse("Action should be disabled when no project", presentation.isEnabledAndVisible)
    }

    // ==================== DatamancerDebugInfoCollector Tests ====================

    fun testCollectorCanBeInstantiated() {
        val collector = DatamancerDebugInfoCollector(project)
        assertNotNull(collector)
    }

    fun testCollectorProducesNonEmptyOutput() {
        val collector = DatamancerDebugInfoCollector(project)
        val output = collector.collectAll(null, null, null)

        assertNotNull(output)
        assertTrue("Output should not be empty", output.isNotEmpty())
    }

    fun testCollectorOutputContainsHeader() {
        val collector = DatamancerDebugInfoCollector(project)
        val output = collector.collectAll(null, null, null)

        assertTrue("Output should contain header", output.contains("Datamancer Debug Information"))
    }

    fun testCollectorOutputContainsEnvironmentSection() {
        val collector = DatamancerDebugInfoCollector(project)
        val output = collector.collectAll(null, null, null)

        assertTrue("Output should contain Generated timestamp", output.contains("Generated:"))
        assertTrue("Output should contain Plugin Version", output.contains("Plugin Version:"))
        assertTrue("Output should contain IDE info", output.contains("IDE:"))
        assertTrue("Output should contain Platform info", output.contains("Platform:"))
    }

    fun testCollectorOutputContainsWorkspaceSection() {
        val collector = DatamancerDebugInfoCollector(project)
        val output = collector.collectAll(null, null, null)

        assertTrue("Output should contain WORKSPACE & PROJECTS section", output.contains("WORKSPACE & PROJECTS"))
    }

    fun testCollectorOutputContainsSourcesSection() {
        val collector = DatamancerDebugInfoCollector(project)
        val output = collector.collectAll(null, null, null)

        assertTrue("Output should contain INDEXED SOURCES section", output.contains("INDEXED SOURCES"))
    }

    fun testCollectorOutputContainsVariablesSection() {
        val collector = DatamancerDebugInfoCollector(project)
        val output = collector.collectAll(null, null, null)

        assertTrue("Output should contain INDEXED VARIABLES section", output.contains("INDEXED VARIABLES"))
    }

    fun testCollectorOutputContainsFooter() {
        val collector = DatamancerDebugInfoCollector(project)
        val output = collector.collectAll(null, null, null)

        assertTrue("Output should contain footer", output.contains("END OF DEBUG INFO"))
    }

    fun testCollectorOutputContainsProjectName() {
        val collector = DatamancerDebugInfoCollector(project)
        val output = collector.collectAll(null, null, null)

        assertTrue("Output should contain Project name", output.contains("Project:"))
    }

    fun testCollectorWithFileOpen() {
        // Create a test file
        val psiFile = myFixture.configureByText("test.sql", "SELECT * FROM customers")
        val collector = DatamancerDebugInfoCollector(project)

        val output = collector.collectAll(psiFile.virtualFile, null, null)

        assertTrue("Output should contain CURRENT FILE INFO section", output.contains("CURRENT FILE INFO"))
        assertTrue("Output should contain file name", output.contains("test.sql"))
    }

    fun testCollectorWithNoFileOpen() {
        val collector = DatamancerDebugInfoCollector(project)

        val output = collector.collectAll(null, null, null)

        assertFalse("Output should not contain file info when no file", output.contains("CURRENT FILE INFO"))
    }

    fun testCollectorOutputHasCorrectSeparators() {
        val collector = DatamancerDebugInfoCollector(project)
        val output = collector.collectAll(null, null, null)

        val separatorCount = output.split("====").size - 1
        assertTrue("Output should have multiple separator lines", separatorCount >= 10)
    }

    fun testCollectorHandlesNullEvent() {
        val collector = DatamancerDebugInfoCollector(project)
        val output = collector.collectAll(null, null, null)

        // Should not contain CURRENT FILE INFO section when event is null
        assertFalse(
            "Output should not contain file info when event is null",
            output.contains("CURRENT FILE INFO")
        )
    }

    // ==================== Helper Methods ====================

    private fun createDataContextWithProject(): DataContext {
        return DataContext { dataId ->
            when (dataId) {
                CommonDataKeys.PROJECT.name -> project
                else -> null
            }
        }
    }

    private fun createDataContextWithFile(virtualFile: com.intellij.openapi.vfs.VirtualFile): DataContext {
        return DataContext { dataId ->
            when (dataId) {
                CommonDataKeys.PROJECT.name -> project
                CommonDataKeys.VIRTUAL_FILE.name -> virtualFile
                else -> null
            }
        }
    }
}

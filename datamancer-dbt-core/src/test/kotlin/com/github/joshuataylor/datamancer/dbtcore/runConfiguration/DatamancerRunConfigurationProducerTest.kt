package com.github.joshuataylor.datamancer.dbtcore.runConfiguration

import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for DatamancerRunConfigurationProducer.
 * Tests the getDbtModelPath logic via reflection since the method is private.
 */
class DatamancerRunConfigurationProducerTest : BasePlatformTestCase() {

    private val producer = DatamancerRunConfigurationProducer()

    /**
     * Invokes the private getDbtModelPath method via reflection.
     */
    private fun invokeGetDbtModelPath(psiFile: PsiFile, virtualFile: VirtualFile): String? {
        val method = DatamancerRunConfigurationProducer::class.java.getDeclaredMethod(
            "getDbtModelPath", PsiFile::class.java, VirtualFile::class.java
        )
        method.isAccessible = true
        return method.invoke(producer, psiFile, virtualFile) as? String
    }

    fun testProducerCanBeInstantiated() {
        assertNotNull(DatamancerRunConfigurationProducer())
    }

    fun testProducerExtendsLazyRunConfigurationProducer() {
        assertTrue(
            "Should extend LazyRunConfigurationProducer",
            producer is LazyRunConfigurationProducer<*>
        )
    }

    fun testGetConfigurationFactoryReturnsFactory() {
        val factory = producer.configurationFactory
        assertNotNull("Should return a configuration factory", factory)
    }

    // getDbtModelPath tests
    fun testGetDbtModelPathSimpleModel() {
        val psiFile = myFixture.addFileToProject("models/my_model.sql", "SELECT 1")
        val result = invokeGetDbtModelPath(psiFile, psiFile.virtualFile)
        assertEquals("my_model", result)
    }

    fun testGetDbtModelPathNestedModel() {
        val psiFile = myFixture.addFileToProject("models/staging/my_model.sql", "SELECT 1")
        val result = invokeGetDbtModelPath(psiFile, psiFile.virtualFile)
        assertEquals("staging/my_model", result)
    }

    fun testGetDbtModelPathDeeplyNestedModel() {
        val psiFile = myFixture.addFileToProject("models/marts/core/my_model.sql", "SELECT 1")
        val result = invokeGetDbtModelPath(psiFile, psiFile.virtualFile)
        assertEquals("marts/core/my_model", result)
    }

    fun testGetDbtModelPathReturnsNullForNonSqlFile() {
        val psiFile = myFixture.addFileToProject("models/my_model.py", "print('hello')")
        val result = invokeGetDbtModelPath(psiFile, psiFile.virtualFile)
        assertNull("Should return null for non-SQL files", result)
    }

    fun testGetDbtModelPathReturnsNullForFileNotInModels() {
        val psiFile = myFixture.addFileToProject("macros/my_macro.sql", "SELECT 1")
        val result = invokeGetDbtModelPath(psiFile, psiFile.virtualFile)
        assertNull("Should return null for files not in models/ directory", result)
    }

    fun testGetDbtModelPathReturnsNullForSeedsDirectory() {
        val psiFile = myFixture.addFileToProject("seeds/my_seed.sql", "SELECT 1")
        val result = invokeGetDbtModelPath(psiFile, psiFile.virtualFile)
        assertNull("Should return null for files in seeds/ directory", result)
    }

    fun testGetDbtModelPathReturnsNullForYamlFile() {
        val psiFile = myFixture.addFileToProject("models/schema.yml", "version: 2")
        val result = invokeGetDbtModelPath(psiFile, psiFile.virtualFile)
        assertNull("Should return null for non-SQL files", result)
    }

    fun testGetDbtModelPathStripsExtension() {
        val psiFile = myFixture.addFileToProject("models/orders.sql", "SELECT * FROM orders")
        val result = invokeGetDbtModelPath(psiFile, psiFile.virtualFile)
        assertFalse("Result should not contain .sql extension", result?.contains(".sql") ?: false)
    }
}

package com.github.joshuataylor.datamancer.dbtcore.compile

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for DatamancerCompileService.
 * Verifies service instantiation, singleton pattern, and resource extraction.
 *
 * Note: The compileModel() suspend function requires a Python SDK and dbt
 * installation, so it is not tested here. JSON parsing of CompiledDbtModel
 * is covered separately in CompiledDbtModelTest.
 */
class DatamancerCompileServiceTest : BasePlatformTestCase() {

    fun testServiceInstanceCreation() {
        val service = DatamancerCompileService.getInstance(project)
        assertNotNull("Service should be available", service)
    }

    fun testServiceIsSingleton() {
        val service1 = DatamancerCompileService.getInstance(project)
        val service2 = DatamancerCompileService.getInstance(project)
        assertSame("Should return same instance", service1, service2)
    }

    fun testServiceExistsInProject() {
        val service = project.getService(DatamancerCompileService::class.java)
        assertNotNull("Service should be registered as project service", service)
    }

    fun testBundledPythonScriptResourceExists() {
        val resource = DatamancerCompileService::class.java.getResourceAsStream("/dbt/dbtcompile.py")
        assertNotNull("Bundled Python script /dbt/dbtcompile.py should exist on classpath", resource)
        resource?.close()
    }

    fun testExtractPythonScriptCreatesFile() {
        val service = DatamancerCompileService.getInstance(project)
        // Access the lazy pythonScript property via reflection
        val field = DatamancerCompileService::class.java.getDeclaredField("pythonScript\$delegate")
        field.isAccessible = true
        val lazyDelegate = field.get(service) as Lazy<*>
        val scriptFile = lazyDelegate.value as java.io.File
        assertTrue("Extracted script file should exist", scriptFile.exists())
        assertTrue("Extracted script file should not be empty", scriptFile.length() > 0)
    }

    fun testExtractPythonScriptFileHasPyExtension() {
        val service = DatamancerCompileService.getInstance(project)
        val field = DatamancerCompileService::class.java.getDeclaredField("pythonScript\$delegate")
        field.isAccessible = true
        val lazyDelegate = field.get(service) as Lazy<*>
        val scriptFile = lazyDelegate.value as java.io.File
        assertTrue("Script file should have .py extension", scriptFile.name.endsWith(".py"))
    }

    fun testCompileModelMethodExists() {
        // Kotlin suspend functions are compiled with a Continuation parameter
        val method = DatamancerCompileService::class.java.methods.find {
            it.name == "compileModel" ||
                it.name.startsWith("compileModel")
        }
        assertNotNull("compileModel method should exist", method)
    }
}

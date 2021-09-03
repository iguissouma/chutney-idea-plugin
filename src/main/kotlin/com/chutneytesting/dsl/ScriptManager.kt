/*
package com.chutneytesting.dsl


import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderEnumerator
import org.jetbrains.kotlin.cli.common.repl.KotlinJsr223JvmScriptEngineFactoryBase
import org.jetbrains.kotlin.cli.common.repl.ScriptArgsWithTypes
import org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmLocalScriptEngine
import org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmLocalScriptEngineFactory
import org.jetbrains.kotlin.script.jsr223.KotlinStandardJsr223ScriptTemplate
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.FileSystemNotFoundException
import java.nio.file.Paths
import javax.script.Bindings
import javax.script.ScriptContext
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.fileExtension
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.JvmScriptCompilationConfigurationBuilder
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.updateClasspath
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate
import kotlin.script.experimental.jvmhost.createJvmEvaluationConfigurationFromTemplate
import kotlin.script.experimental.jvmhost.jsr223.KotlinJsr223ScriptEngineImpl

object ScriptManager: ScriptEngineManager() {
    init {
        if (getEngineByExtension("kts") == null) {
            registerEngineExtension("kts", KotlinJsr223JvmLocalScriptEngineFactory())
        }
        if (getEngineByExtension("chutney.kts") == null) {
            registerEngineExtension("chutney.kts", KotlinJsr223ChutneyKtsScriptEngineFactory())
        }
    }

    fun evalFile(scriptFile: File): ResultWithDiagnostics<EvaluationResult> {


        val compilationConfiguration = createJvmCompilationConfigurationFromTemplate<ChutneyFileScript>()
        val evaluationConfiguration = createJvmEvaluationConfigurationFromTemplate<ChutneyFileScript>()

        return BasicJvmScriptingHost().eval(scriptFile.toScriptSource(), compilationConfiguration, evaluationConfiguration)
    }


}


class KotlinJsr223ChutneyKtsScriptEngineFactory : KotlinJsr223JvmScriptEngineFactoryBase() {

    private val compilationConfiguration = createJvmCompilationConfigurationFromTemplate<ChutneyFileScript>()
    private val evaluationConfiguration = createJvmEvaluationConfigurationFromTemplate<ChutneyFileScript>()
    private var lastClassLoader: ClassLoader? = null
    private var lastClassPath: List<File>? = null

    override fun getExtensions(): List<String> = listOf(compilationConfiguration[ScriptCompilationConfiguration.fileExtension]!!)

    @Synchronized
    protected fun JvmScriptCompilationConfigurationBuilder.dependenciesFromCurrentContext() {
        val currentClassLoader = Thread.currentThread().contextClassLoader
        val classPath = listOf<File>()
        */
/*if (lastClassLoader == null || lastClassLoader != currentClassLoader) {
            scriptCompilationClasspathFromContext(
                classLoader = currentClassLoader,
                wholeClasspath = true,
                unpackJarCollections = true
            ).also {
                lastClassLoader = currentClassLoader
                lastClassPath = it
            }
        } else lastClassPath!!*//*

        val classpathIdeaPlugin = File(ChutneyPluginPaths.chutneyIdeaPluginLibPath).listFiles()
            ?.filter { it.canonicalPath.endsWith(".jar") }
            ?.map { it }
            ?.toMutableList()!!
        updateClasspath(classpathIdeaPlugin + classPath)
    }

    override fun getScriptEngine(): ScriptEngine {
        return KotlinJsr223ScriptEngineImpl(
            this,
            ScriptCompilationConfiguration(compilationConfiguration) {
                jvm {
                    dependenciesFromCurrentContext()
                }
            },
            evaluationConfiguration
        ) { ScriptArgsWithTypes(arrayOf(emptyArray<String>()), arrayOf(Array<String>::class)) }
    }
}
*/

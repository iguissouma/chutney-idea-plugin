package com.chutneytesting.idea.runner
/*

import com.chutneytesting.dsl.ChutneyFileScript
import com.chutneytesting.dsl.ScriptManager
import com.chutneytesting.idea.ChutneyUtil
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.annotations.NotNull
import org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmLocalScriptEngineFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate
import kotlin.script.experimental.jvmhost.createJvmEvaluationConfigurationFromTemplate

class ChutneyDslLineMarkerProvider : ChutneyLineMarkerProvider() {
    override fun getLineMarkerInfo(psiElement: PsiElement): LineMarkerInfo<*>? {
        val jsonPsi = psiElement.containingFile
        if (!ChutneyUtil.isChutneyDsl(jsonPsi)) {
            return null
        }
        if (ScriptManager.getEngineByExtension("kts") == null) {
            ScriptManager.registerEngineExtension("kts", KotlinJsr223JvmLocalScriptEngineFactory())
        }
        val scriptContent = "5 + 10"
        //val fromScript: Int = KtsObjectLoader(this.javaClass.classLoader).load<Int>(scriptContent)
        //println(fromScript)
        */
/*with(ScriptManager.getEngineByExtension("kts")) {
            eval("val x = 3")
            val res2 = eval("x + 2")
            // assertEquals(5, res2)
        }*//*

        // SpringBootKotlinScriptEngineFactory().scriptEngine.also { println(it.eval(scriptContent)) }

        //val res = ChutneyKotlinScriptEngineFactory.factory.scriptEngine.eval(psiElement.containingFile.text)
        //val chutneyScenario = KtsObjectLoader().load<ChutneyFileScript.ChutneyScenario>(psiElement.containingFile.text)
        //println(res)
        //
        */
/* val kotlinDynamicCompiler = KotlinDynamicCompiler()
         var errorCount = 0
         val classLoaders = mutableListOf<ClassLoader>()

         try {
             val outputDir = Files.createTempDirectory("out").toFile()
             val testPack = "/testPack$1";
             val sourcePack = javaClass.getResource(testPack).file
             kotlinDynamicCompiler.compileModule("testPack$1", listOf(sourcePack), outputDir, Thread.currentThread().contextClassLoader);
             val uri = arrayOf(outputDir.toURI().toURL())
             val classLoader = URLClassLoader.newInstance(uri)
             classLoaders.add(classLoader)
             classLoader.loadClass("com.example.kt.SimpleClass")
             var str = javaClass.getResourceAsStream("/script$1.kts").reader().use {
                 it.readText()
             }
             val oldCl = Thread.currentThread().contextClassLoader
             try {
                 Thread.currentThread().contextClassLoader = classLoader
                 val engine = KotlinJsr223JvmLocalScriptEngineFactory().scriptEngine
                 engine.eval(str)
             } finally {
                 Thread.currentThread().contextClassLoader = oldCl
             }

         } catch (e: Exception) {
             e.printStackTrace()
         }*//*

        fun evalFile(scriptFile: File): ResultWithDiagnostics<EvaluationResult> {

            val compilationConfiguration = createJvmCompilationConfigurationFromTemplate<ChutneyFileScript>()
            val evaluationConfiguration = createJvmEvaluationConfigurationFromTemplate<ChutneyFileScript>()

            return BasicJvmScriptingHost().eval(
                scriptFile.toScriptSource(),
                compilationConfiguration,
                evaluationConfiguration
            )
        }
        //val evalFile = evalFile(File(psiElement.containingFile.virtualFile.path))


        //
        if ((psiElement is LeafPsiElement) && (psiElement.text == "Scenario")) {
            val displayName = "scenario"
            //val engine = ScriptManager.getEngineByExtension("chutney.kts")!!
            //val context = SimpleScriptContext()
            //val captureOut = captureOut {
            //    evalFile(File(psiElement.containingFile.virtualFile.path)).valueOrThrow().returnValue
            //}
             return lineMarkerInfo(psiElement, displayName)

        }
        return null
    }

    override fun collectSlowLineMarkers(
        elements:  MutableList<out PsiElement>,
        result:  MutableCollection<in LineMarkerInfo<*>>
    ) {
    }

    internal fun captureOut(body: () -> Unit): String = captureOutAndErr(body).first

    internal fun captureOutAndErr(body: () -> Unit): Pair<String, String> {
        val outStream = ByteArrayOutputStream()
        val errStream = ByteArrayOutputStream()
        val prevOut = System.out
        val prevErr = System.err
        System.setOut(PrintStream(outStream))
        System.setErr(PrintStream(errStream))
        try {
            body()
        } finally {
            System.out.flush()
            System.err.flush()
            System.setOut(prevOut)
            System.setErr(prevErr)
        }
        return outStream.toString().trim() to errStream.toString().trim()
    }
}
*/

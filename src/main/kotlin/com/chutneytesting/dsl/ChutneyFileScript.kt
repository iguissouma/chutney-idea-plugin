package com.chutneytesting.dsl

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.mainKts.impl.IvyResolver
import org.jetbrains.kotlin.mainKts.impl.resolveFromAnnotations
import org.jetbrains.kotlin.script.util.CompilerOptions
import org.jetbrains.kotlin.script.util.DependsOn
import org.jetbrains.kotlin.script.util.Import
import org.jetbrains.kotlin.script.util.Repository
import java.io.File
import java.security.MessageDigest
import kotlin.script.dependencies.ScriptContents
import kotlin.script.dependencies.ScriptDependenciesResolver
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.dependencies.CompoundDependenciesResolver
import kotlin.script.experimental.dependencies.FileSystemDependenciesResolver
import kotlin.script.experimental.host.FileBasedScriptSource
import kotlin.script.experimental.host.FileScriptSource
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.jvm.*
import kotlin.script.experimental.jvm.compat.mapLegacyDiagnosticSeverity
import kotlin.script.experimental.jvm.compat.mapLegacyScriptPosition
import kotlin.script.experimental.jvmhost.CompiledScriptJarsCache
import kotlin.script.experimental.jvmhost.jsr223.configureProvidedPropertiesFromJsr223Context
import kotlin.script.experimental.jvmhost.jsr223.importAllBindings
import kotlin.script.experimental.jvmhost.jsr223.jsr223

@Suppress("unused")
@KotlinScript(
    displayName = "Chutney Script",
    fileExtension = "chutney.kts",
    compilationConfiguration = ChutneyKtsScriptDefinition::class,
    evaluationConfiguration = ChutneyScriptEvaluationConfiguration::class
)
abstract class ChutneyFileScript(val args: Array<String>)

@DslMarker
annotation class ChutneyScenarioDsl

fun Scenario(title: String, block: ChutneyScenarioBuilder.() -> Unit): String {
    return ChutneyScenarioBuilder(title).apply(block).build().toString()
}

@ChutneyScenarioDsl
class ChutneyScenarioBuilder(val title: String = "") {

    private val givens = mutableListOf<ChutneyStep>()
    private var `when`: ChutneyStep? = null
    private val thens = mutableListOf<ChutneyStep>()

    fun Given(description: String = "", block: ChutneyStepBuilder.() -> Unit) {
        givens.add(ChutneyStepBuilder(description).apply(block).build())
    }

    fun When(description: String = "", block: ChutneyStepBuilder.() -> Unit) {
        `when` = ChutneyStepBuilder(description).apply(block).build()
    }

    fun Then(description: String = "", block: ChutneyStepBuilder.() -> Unit) {
        thens.add(ChutneyStepBuilder(description).apply(block).build())
    }

    fun And(description: String = "", block: ChutneyStepBuilder.() -> Unit) {
        when {
            `when` != null -> thens.add(ChutneyStepBuilder(description).apply(block).build())
            else -> givens.add(ChutneyStepBuilder(description).apply(block).build())
        }
    }

    fun build(): ChutneyScenario = ChutneyScenario(title, givens, `when`, thens)

}

@ChutneyScenarioDsl
class ChutneyStepBuilder(var description: String = "") {

    var subSteps = mutableListOf<ChutneyStep>()
    var implementation: ChutneyStepImpl? = null

    fun Implementation(block: ChutneyStepImplBuilder.() -> Unit) {
        implementation = ChutneyStepImplBuilder().apply(block).build()
    }

    fun Step(description: String = "", block: ChutneyStepBuilder.() -> Unit) {
        subSteps.add(ChutneyStepBuilder(description).apply(block).build())
    }

    fun ContextPutTask(entries: Map<String, Any>, outputs: Map<String, Any> = mapOf()) {
        implementation =
            ChutneyStepImpl(type = "context-put", target = null, inputs = entries.toEntries(), outputs = outputs)
    }

    fun DebugTask() {
        implementation = ChutneyStepImpl(type = "debug", target = null, inputs = mapOf(), outputs = mapOf())
    }

    fun HttpGetTask(target: String, uri: String, headers: Map<String, Any> = mapOf(), timeout: String = "2 sec") {
        implementation = ChutneyStepImpl(
            type = "http-get",
            target = target,
            inputs = mapOf("uri" to uri, "headers" to headers, "timeout" to timeout),
            outputs = mapOf()
        )
    }

    fun JsonAssertTask(document: String, expected: Map<String, Any> = mapOf()) {
        implementation = ChutneyStepImpl(
            type = "json-assert",
            target = null,
            inputs = mapOf("document" to document, "expected" to expected),
            outputs = mapOf()
        )
    }

    fun build(): ChutneyStep = ChutneyStep(description, implementation, subSteps)

}

@ChutneyScenarioDsl
class ChutneyStepImplBuilder {

    var type: String = ""
    var target: String = ""
    var inputs: Map<String, Any> = mapOf()
    var outputs: Map<String, Any> = mapOf()

    fun build(): ChutneyStepImpl = ChutneyStepImpl(type, target, inputs, outputs)

}


fun main() {
    val prm = "30000111898410"
    val x = Scenario("Consulter les caractéristiques d'un PRM") {
        Given("le PRM $prm") {
            Step("substeps sample") {
                ContextPutTask(mapOf("idPrm" to prm))
            }
        }
        When("on demande les caractéristiques du PRM") {
            HttpGetTask(target = "ICOEUR_REFERENTIEL_CERTIF_IPARC", uri = "/api/v1/compteurs/${"idPrm".spEL()}")
        }
        Then("les informations renvoyées sont cohérentes") {
            JsonAssertTask(document = json("body".spELVar), expected = mapOf("$.prm" to "idPrm".spEL()))
        }
    }
    println(x)
}

const val JSON_PATH_ROOT = "\$"
val String.spELVar: String
    get() = "#$this"
val String.spEL: String
    get() = "\${#$this}"

public fun String.spEL(): String = "\${#$this}"
public fun String.spELVar(): String = "#$this"
public fun Map<String, Any>.toEntries(): Map<String, Map<String, Any>> {
    return mapOf("entries" to this)
}
fun json(variable: String, path: String = JSON_PATH_ROOT): String = "json(${variable.spELVar}, '$path')".spEL()

object Mapper {
    val mapper = ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .enable(SerializationFeature.INDENT_OUTPUT)
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
}

interface Task
class ContextPutTask(val entries: Map<String, String> = mapOf()) : Task
class ChutneyStep(
    val description: String,
    val implementation: ChutneyStepImpl? = null,
    val subSteps: List<ChutneyStep>? = null
)

class ChutneyStepImpl(
    val type: String,
    val target: String?,
    val inputs: Map<String, Any>,
    val outputs: Map<String, Any>
)

class ChutneyScenario(
    val title: String = "",
    val givens: List<ChutneyStep> = mutableListOf<ChutneyStep>(),
    val `when`: ChutneyStep? = null,
    val thens: List<ChutneyStep> = mutableListOf<ChutneyStep>()
) {

    override fun toString(): String {
        return Mapper.mapper.writeValueAsString(this)
    }
}

object ChutneyScriptEvaluationConfiguration : ScriptEvaluationConfiguration(
    {
        scriptsInstancesSharing(true)
        refineConfigurationBeforeEvaluate(::configureProvidedPropertiesFromJsr223Context)
        refineConfigurationBeforeEvaluate(::configureConstructorArgsFromMainArgs)
    }
)

fun configureConstructorArgsFromMainArgs(context: ScriptEvaluationConfigurationRefinementContext): ResultWithDiagnostics<ScriptEvaluationConfiguration> {
    val res =
        context.evaluationConfiguration.with {
            constructorArgs(emptyArray<String>())
        }
    return res.asSuccess()
}

const val COMPILED_SCRIPTS_CACHE_DIR_ENV_VAR = "KOTLIN_CHUTNEY_KTS_COMPILED_SCRIPTS_CACHE_DIR"
const val COMPILED_SCRIPTS_CACHE_DIR_PROPERTY = "kotlin.chutney.kts.compiled.scripts.cache.dir"

class ChutneyKtsScriptDefinition : ScriptCompilationConfiguration(
    {
        defaultImports(DependsOn::class, Repository::class, Import::class, CompilerOptions::class)
        jvm {
            dependenciesFromClassContext(
                ChutneyKtsScriptDefinition::class,
                "kotlin-main-kts",
                "kotlin-stdlib",
                "kotlin-reflect"
            )
        }
        dependencies(
            File(ChutneyPluginPaths.chutneyIdeaPluginLibPath).listFiles()?.toList()
                //?.filter { !it.path.contains("kotlin") }
                ?.map { JvmDependency(it) }
                ?: emptyList()
        )
        refineConfiguration {
            onAnnotations(
                DependsOn::class,
                Repository::class,
                Import::class,
                CompilerOptions::class,
                handler = ChutneyKtsConfigurator()
            )
            beforeCompiling(::configureProvidedPropertiesFromJsr223Context)
        }
        ide {
            acceptedLocations(ScriptAcceptedLocation.Everywhere)
        }
        jsr223 {
            importAllBindings(true)
        }
        hostConfiguration(ScriptingHostConfiguration {
            jvm {
                val cacheExtSetting = System.getProperty(COMPILED_SCRIPTS_CACHE_DIR_PROPERTY)
                    ?: System.getenv(COMPILED_SCRIPTS_CACHE_DIR_ENV_VAR)
                val cacheBaseDir = when {
                    cacheExtSetting == null -> System.getProperty("java.io.tmpdir")
                        ?.let(::File)?.takeIf { it.exists() && it.isDirectory }
                        ?.let { File(it, "chutney.kts.compiled.cache").apply { mkdir() } }
                    cacheExtSetting.isBlank() -> null
                    else -> File(cacheExtSetting)
                }?.takeIf { it.exists() && it.isDirectory }
                if (cacheBaseDir != null)
                    compilationCache(
                        CompiledScriptJarsCache { script, scriptCompilationConfiguration ->
                            File(
                                cacheBaseDir, compiledScriptUniqueName(
                                    script,
                                    scriptCompilationConfiguration
                                ) + ".jar"
                            )
                        }
                    )
            }
        })
    })

class ChutneyKtsConfigurator : RefineScriptCompilationConfigurationHandler {
    private val resolver = CompoundDependenciesResolver(FileSystemDependenciesResolver(), IvyResolver())

    override operator fun invoke(context: ScriptConfigurationRefinementContext): ResultWithDiagnostics<ScriptCompilationConfiguration> =
        processAnnotations(context)

    fun processAnnotations(context: ScriptConfigurationRefinementContext): ResultWithDiagnostics<ScriptCompilationConfiguration> {
        val diagnostics = arrayListOf<ScriptDiagnostic>()

        fun report(
            severity: ScriptDependenciesResolver.ReportSeverity,
            message: String,
            position: ScriptContents.Position?
        ) {
            diagnostics.add(
                ScriptDiagnostic(
                    message,
                    mapLegacyDiagnosticSeverity(severity),
                    context.script.locationId,
                    mapLegacyScriptPosition(position)
                )
            )
        }

        val annotations = context.collectedData?.get(ScriptCollectedData.foundAnnotations)?.takeIf { it.isNotEmpty() }
            ?: return context.compilationConfiguration.asSuccess()

        val scriptBaseDir = (context.script as? FileBasedScriptSource)?.file?.parentFile
        val importedSources = annotations.flatMap {
            (it as? Import)?.paths?.map { sourceName ->
                FileScriptSource(scriptBaseDir?.resolve(sourceName) ?: File(sourceName))
            } ?: emptyList()
        }
        val compileOptions = annotations.flatMap {
            (it as? CompilerOptions)?.options?.toList() ?: emptyList()
        }

        val resolveResult = try {
            runBlocking {
                resolveFromAnnotations(resolver, annotations.filter { it is DependsOn || it is Repository })
            }
        } catch (e: Throwable) {
            ResultWithDiagnostics.Failure(
                *diagnostics.toTypedArray(),
                e.asDiagnostics(path = context.script.locationId)
            )
        }

        return resolveResult.onSuccess { resolvedClassPath ->
            ScriptCompilationConfiguration(context.compilationConfiguration) {
                if (resolvedClassPath != null) updateClasspath(resolvedClassPath)
                if (importedSources.isNotEmpty()) importScripts.append(importedSources)
                if (compileOptions.isNotEmpty()) compilerOptions.append(compileOptions)
            }.asSuccess()
        }
    }
}


private fun compiledScriptUniqueName(
    script: SourceCode,
    scriptCompilationConfiguration: ScriptCompilationConfiguration
): String {
    val digestWrapper = MessageDigest.getInstance("MD5")
    digestWrapper.update(script.text.toByteArray())
    scriptCompilationConfiguration.notTransientData.entries
        .sortedBy { it.key.name }
        .forEach {
            digestWrapper.update(it.key.name.toByteArray())
            digestWrapper.update(it.value.toString().toByteArray())
        }
    return digestWrapper.digest().toHexString()
}

private fun ByteArray.toHexString(): String = joinToString("", transform = { "%02x".format(it) })


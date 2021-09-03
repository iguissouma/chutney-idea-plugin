package com.chutneytesting.dsl
import com.intellij.openapi.application.PathManager.getPluginsPath
import com.intellij.util.PathUtil.toSystemIndependentName

/*
package com.chutneytesting.dsl

import com.intellij.openapi.application.PathManager.getPluginsPath
import com.intellij.util.PathUtil.toSystemIndependentName
import java.io.File
import kotlin.script.experimental.intellij.ScriptDefinitionsProvider

class ScenarioKotlinScriptProvider : ScriptDefinitionsProvider {
    override val id = "ScenarioKotlinScriptProvider"
    override fun getDefinitionClasses() = listOf(ChutneyFileScript::class.java.canonicalName)
    override fun getDefinitionsClassPath() = File(ChutneyPluginPaths.chutneyIdeaPluginLibPath).listFiles()?.toList().also { println("chutneyIdeaPluginLibPath == ${it.toString()}") } ?: emptyList()
    override fun useDiscovery() = true
}

object ChutneyPluginPaths {
    val chutneyIdeaPluginLibPath = toSystemIndependentName(getPluginsPath() + "/chutney-idea-plugin/lib/")
}
*/

object ChutneyPluginPaths {
    val chutneyIdeaPluginLibPath = toSystemIndependentName(getPluginsPath() + "/chutney-idea-plugin/lib/")
}

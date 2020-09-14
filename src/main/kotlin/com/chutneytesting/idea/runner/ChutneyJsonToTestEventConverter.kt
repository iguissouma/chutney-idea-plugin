package com.chutneytesting.idea.runner


import com.chutneytesting.idea.server.ChutneyServer
import com.chutneytesting.idea.server.ChutneyServerRegistry
import com.chutneytesting.idea.util.WaitUntilUtils
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.testframework.TestConsoleProperties
import com.intellij.execution.testframework.sm.runner.OutputToGeneralTestEventsConverter
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class ChutneyJsonToTestEventConverter(
    testFrameworkName: String,
    consoleProperties: TestConsoleProperties,
    val configuration: ChutneyRunConfiguration,
    val project: Project,
    val processHandler: ProcessHandler,
    val jsonFiles: List<VirtualFile?>
) : OutputToGeneralTestEventsConverter(testFrameworkName, consoleProperties) {

    override fun onStartTesting() {
        ApplicationManager.getApplication().executeOnPooledThread {
            val server = ChutneyServerRegistry.instance.myServer
            WaitUntilUtils.waitUntil({ getServerUrl(server) != null }, 30000)
            processHandler.startNotify()
            jsonFiles.filterNotNull().forEachIndexed { index, virtualFile ->
                val parser = JsonTestScenariosParser(configuration, project, processHandler, virtualFile)
                parser.parseScenarios(index + 1)
            }
            jsonFiles.filterNotNull().forEachIndexed { index, virtualFile ->
                val parser = JsonTestReportsParser(
                    configuration,
                    project,
                    getServerUrl(ChutneyServerRegistry.instance.myServer)!!,
                    processHandler,
                    virtualFile
                )
                parser.parseReports(index + 1)
            }
            processHandler.detachProcess()
        }
    }

    private fun getServerUrl(ideServer: ChutneyServer?): String? {
        if (configuration.getRunSettings().isExternalServerType()) {
            return configuration.getRunSettings().serverAddress
        }
        return if (ideServer != null && ideServer.isReadyForRunningTests) {
            ideServer.serverUrl
        } else null
    }
}

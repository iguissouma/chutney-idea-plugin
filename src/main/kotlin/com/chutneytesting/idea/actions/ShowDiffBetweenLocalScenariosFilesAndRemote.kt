package com.chutneytesting.idea.actions

import com.chutneytesting.idea.ChutneyUtil
import com.chutneytesting.idea.ChutneyUtil.getChutneyScenarioIdFromFileName
import com.chutneytesting.idea.ChutneyUtil.isRemoteChutneyJson
import com.chutneytesting.idea.actions.converter.ScenarioV2ToV1Converter
import com.chutneytesting.idea.logger.EventDataLogger
import com.chutneytesting.idea.runner.settings.ChutneySettingsUtil
import com.chutneytesting.idea.util.ChutneyServerApiUtils
import com.chutneytesting.idea.util.ChutneyServerApiUtils.checkRemoteServerUrlConfig
import com.chutneytesting.idea.util.HJsonUtils
import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffDialogHints
import com.intellij.diff.chains.SimpleDiffRequestChain
import com.intellij.diff.impl.CacheDiffRequestChainProcessor
import com.intellij.diff.impl.DiffWindow
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.json.psi.JsonElementGenerator
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.psi.PsiManager
import com.intellij.psi.codeStyle.CodeStyleManager


class ShowDiffBetweenLocalScenariosFilesAndRemote : AnAction() {

    private val LOG = Logger.getInstance(ShowDiffBetweenLocalScenariosFilesAndRemote::class.java)

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        if (!checkRemoteServerUrlConfig(project)) return
        val file = event.getData(DataKeys.VIRTUAL_FILE) ?: return
        if (!file.isDirectory) {
            return
        }

        val scenariosInDirectory = ChutneySettingsUtil
            .collectChutneyScenarioFilesInDirectory(project, file)
            .filter { isRemoteChutneyJson(it) }
            .map { getChutneyScenarioIdFromFileName(it.name)!! to it }
            .toMap()

        try {
            val list = scenariosInDirectory.map {
                val query = ChutneyServerApiUtils.getRemoteDatabaseUrl()
                val body = "(select * from scenario where id = '${it.key}')"
                var remoteScenario = ""
                val result = ChutneyServerApiUtils.post<Base>(query, body)
                result.table?.rows?.forEach { rows ->
                    val version = rows.values?.get(8)
                    val rawScenario = rows.values?.get(3)!!
                    var convertedScenario = rawScenario
                    if (ChutneyUtil.isChutneyV1Json(PsiManager.getInstance(project).findFile(it.value)!!) && version?.matches(
                            Regex("v2.*")
                        )!!
                    ) {
                        convertedScenario = ScenarioV2ToV1Converter().convert(rawScenario)
                    }
                    remoteScenario = HJsonUtils.convertHjson(convertedScenario)
                }
                val createRemotePsiFileFromText = JsonElementGenerator(project).createDummyFile(remoteScenario)
                val detectedLineSeparator = LoadTextUtil.detectLineSeparator(it.value, true) ?: ""
                val remoteScenarioFileContent =
                    CodeStyleManager.getInstance(project).reformat(createRemotePsiFileFromText)
                        .text.replace(Regex("\\v+"), detectedLineSeparator)
                val content1 = DiffContentFactory.getInstance().create(project, it.value)
                val content2 = DiffContentFactory.getInstance().create(remoteScenarioFileContent)
                val request = SimpleDiffRequest(
                    "Scenario ${it.value.name}",
                    content1,
                    content2,
                    "Local Scenario File",
                    "Remote Scenario File"
                )
                scenariosInDirectory@ request
            }.toList()
            val cacheDiffRequestChainProcessor = CacheDiffRequestChainProcessor(project, SimpleDiffRequestChain(list))
            val diffWindow = DiffWindow(project, cacheDiffRequestChainProcessor.requestChain, DiffDialogHints.FRAME)
            diffWindow.show()

        } catch (e: Exception) {
            LOG.debug(e.toString())
            EventDataLogger.logError(e.toString(), project)
        }

    }

    override fun update(event: AnActionEvent) {
        // Set the availability based on whether a project is open
        val project = event.project
        val virtualFile = event.getData(DataKeys.VIRTUAL_FILE)
        event.presentation.isEnabledAndVisible = project != null && virtualFile != null && virtualFile.isDirectory
    }

}

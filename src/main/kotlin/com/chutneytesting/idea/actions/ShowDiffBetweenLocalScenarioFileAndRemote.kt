package com.chutneytesting.idea.actions

import com.chutneytesting.idea.ChutneyUtil
import com.chutneytesting.idea.ChutneyUtil.getChutneyScenarioIdFromFileName
import com.chutneytesting.idea.actions.converter.ScenarioV2ToV1Converter
import com.chutneytesting.idea.logger.EventDataLogger
import com.chutneytesting.idea.util.ChutneyServerApiUtils
import com.chutneytesting.idea.util.ChutneyServerApiUtils.checkRemoteServerUrlConfig
import com.chutneytesting.idea.util.HJsonUtils
import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.json.psi.JsonElementGenerator
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataKeys
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.codeStyle.CodeStyleManager


class ShowDiffBetweenLocalScenarioFileAndRemote : RemoteScenarioBaseAction() {

    private val LOG = Logger.getInstance(ShowDiffBetweenLocalScenarioFileAndRemote::class.java)

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        if (!checkRemoteServerUrlConfig(project)) return
        val file = event.getData(DataKeys.VIRTUAL_FILE) ?: return
        val psiFile = event.getData(LangDataKeys.PSI_FILE) ?: return
        val id = getChutneyScenarioIdFromFileName(file.name) ?: return
        try {
            val query = ChutneyServerApiUtils.getRemoteDatabaseUrl()
            val body = "(select * from scenario where id = '$id')"
            var remoteScenario = ""
            val result = ChutneyServerApiUtils.post<Base>(query, body)

            result.table?.rows?.forEach {
                val version = it.values?.get(8)
                val rawScenario = it.values?.get(3)!!
                var convertedScenario = rawScenario
                if (ChutneyUtil.isChutneyV1Json(psiFile) && version?.matches(Regex("v2.*"))!!) {
                    convertedScenario = ScenarioV2ToV1Converter().convert(rawScenario)
                }
                remoteScenario = HJsonUtils.convertHjson(convertedScenario)
            }
            val createRemotePsiFileFromText = JsonElementGenerator(project).createDummyFile(remoteScenario)
            val remoteScenarioFileContent =
                CodeStyleManager.getInstance(project).reformat(createRemotePsiFileFromText).text.replace(
                    Regex("\\v+"),
                    file.detectedLineSeparator!!
                )
            val content1 = DiffContentFactory.getInstance().create(project, file)
            val content2 = DiffContentFactory.getInstance().create(remoteScenarioFileContent)
            val request = SimpleDiffRequest(
                "Show Diff Between Local Scenario File and Remote",
                content1,
                content2,
                "Local Scenario File",
                "Remote Scenario File"
            )
            DiffManager.getInstance().showDiff(project, request)

        } catch (e: Exception) {
            LOG.debug(e.toString())
            EventDataLogger.logError(e.toString(), project)
        }

    }

}

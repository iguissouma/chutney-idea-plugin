package com.chutneytesting.idea.actions

import com.chutneytesting.idea.ChutneyUtil
import com.chutneytesting.idea.actions.converter.ScenarioV2ToV1Converter
import com.chutneytesting.idea.logger.EventDataLogger
import com.chutneytesting.idea.util.ChutneyServerApiUtils
import com.chutneytesting.idea.util.ChutneyServerApiUtils.checkRemoteServerUrlConfig
import com.chutneytesting.idea.util.ChutneyServerApiUtils.getRemoteServerUrl
import com.chutneytesting.idea.util.HJsonUtils
import com.intellij.notification.NotificationListener
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataKeys
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.ui.EditorNotifications
import com.intellij.util.ui.UIUtil


class UpdateLocalScenarioFromRemoteServer : RemoteScenarioBaseAction() {

    private val LOG = Logger.getInstance(UpdateLocalScenarioFromRemoteServer::class.java)

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        if (!checkRemoteServerUrlConfig(project)) return
        val file = event.getData(DataKeys.VIRTUAL_FILE) ?: return
        val psiFile = event.getData(LangDataKeys.PSI_FILE) ?: return
        val editor = event.getData(PlatformDataKeys.EDITOR) ?: return
        val document = editor.document
        val id = ChutneyUtil.getChutneyScenarioIdFromFileName(file.name)
        try {
            val query: String = ChutneyServerApiUtils.getRemoteDatabaseUrl()
            val body = "(select * from scenario where id = '$id')"
            val result = ChutneyServerApiUtils.post<Base>(query, body)
            result.table?.rows?.forEach {
                val runnable = Runnable {
                    val version = it.values?.get(8)
                    val rawScenario = it.values?.get(3)!!
                    var convertedScenario = rawScenario
                    if (ChutneyUtil.isChutneyV1Json(psiFile) && version?.matches(Regex("v2.*"))!!) {
                        convertedScenario = ScenarioV2ToV1Converter().convert(rawScenario)
                    }
                    document.setText(HJsonUtils.convertHjson(convertedScenario).replace("\r\n", "\n"))
                    FileDocumentManager.getInstance().saveDocument(document)
                    UIUtil.invokeAndWaitIfNeeded(Runnable {
                        CodeStyleManager.getInstance(project).reformat(psiFile)
                    })
                }
                WriteCommandAction.runWriteCommandAction(project, runnable)
            }
            EventDataLogger.logInfo(
                "Local scenario file updated with success.<br>" +
                        "<a href=\"${getRemoteServerUrl()}/#/scenario/$id/execution/last\">Open in remote Chutney Server</a>",
                project,
                NotificationListener.URL_OPENING_LISTENER
            )

            EditorNotifications.getInstance(project).updateNotifications(file)

        } catch (e: Exception) {
            LOG.debug(e.toString())
            EventDataLogger.logError(e.toString(), project)
        }

    }

}

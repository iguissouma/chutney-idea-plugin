package com.chutneytesting.idea.actions

import com.chutneytesting.idea.ChutneyUtil
import com.chutneytesting.idea.actions.converter.ScenarioV1ToV2Converter
import com.chutneytesting.idea.logger.EventDataLogger
import com.chutneytesting.idea.util.ChutneyServerApiUtils
import com.chutneytesting.idea.util.ChutneyServerApiUtils.checkRemoteServerUrlConfig
import com.chutneytesting.idea.util.ChutneyServerApiUtils.getRemoteServerUrl
import com.intellij.notification.NotificationListener
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataKeys
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.ui.EditorNotifications
import org.apache.commons.lang.StringEscapeUtils
import org.hjson.JsonValue
import org.hjson.Stringify


class UpdateRemoteScenarioFromLocal : RemoteScenarioBaseAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        if (!checkRemoteServerUrlConfig(project)) return
        val file = event.getData(DataKeys.VIRTUAL_FILE) ?: return
        val psiFile = event.getData(LangDataKeys.PSI_FILE) ?: return
        val query = ChutneyServerApiUtils.getRemoteDatabaseUrl()
        val processJsonReference = ChutneyUtil.processJsonReference(psiFile.virtualFile)
        var hJsonString: String? = JsonValue.readHjson(processJsonReference).toString(Stringify.PLAIN)
        if (ChutneyUtil.isChutneyV1Json(psiFile)) {
            hJsonString =
                JsonValue.readHjson(ScenarioV1ToV2Converter().convert(processJsonReference)).toString(Stringify.PLAIN)
        }
        val id = ChutneyUtil.getChutneyScenarioIdFromFileName(file.name)
        val escapeSql = StringEscapeUtils.escapeSql(hJsonString)
        val body = "update scenario set content='$escapeSql', version=2.1 where id = '$id'"
        try {
            ChutneyServerApiUtils.post<Any>(query, body)
            EventDataLogger.logInfo(
                "Remote scenario file updated with success.<br>" +
                        "<a href=\"${getRemoteServerUrl()}/#/scenario/$id/execution/last\">Open in remote Chutney Server</a>",
                project,
                NotificationListener.URL_OPENING_LISTENER
            )

            EditorNotifications.getInstance(project).updateNotifications(file)

        } catch (e: Exception) {
            EventDataLogger.logError(e.toString(), project)
        }
    }

}

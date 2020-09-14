package com.chutneytesting.idea.actions

import com.chutneytesting.idea.ChutneyUtil
import com.chutneytesting.idea.ChutneyUtil.getChutneyScenarioDescriptionFromFileName
import com.chutneytesting.idea.ChutneyUtil.getChutneyScenarioIdFromFileName
import com.chutneytesting.idea.actions.converter.ScenarioV1ToV2Converter
import com.chutneytesting.idea.logger.EventDataLogger
import com.chutneytesting.idea.server.ChutneyServerRegistry
import com.chutneytesting.idea.util.ChutneyServerApiUtils
import com.chutneytesting.idea.util.HJsonUtils
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataKeys
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.diagnostic.Logger
import org.apache.commons.lang.StringEscapeUtils.escapeSql
import org.hjson.JsonValue
import org.hjson.Stringify


class AddScenarioToLocalServer : RemoteScenarioBaseAction() {

    private val LOG = Logger.getInstance(AddScenarioToLocalServer::class.java)

    override fun actionPerformed(event: AnActionEvent) {
        val file = event.getData(DataKeys.VIRTUAL_FILE) ?: return
        val project = event.project ?: return
        val psiFile = event.getData(LangDataKeys.PSI_FILE) ?: return
        val id = getChutneyScenarioIdFromFileName(file.name) ?: return
        val titleAndDescription = getChutneyScenarioDescriptionFromFileName(file.name)
        try {
            val localServerURL = ChutneyServerRegistry.instance.myServer?.serverUrl ?: return
            var content = escapeSql(HJsonUtils.convertHjson(psiFile.text))
            if (ChutneyUtil.isChutneyV1Json(psiFile)) {
                content = JsonValue.readHjson(ScenarioV1ToV2Converter().convert(content)).toString(Stringify.PLAIN)
            }
            val query: String = "$localServerURL/api/v1/admin/database/execute/jdbc"
            val body =
                "insert into scenario(id, title, description, content, version) values($id, '$titleAndDescription', '$titleAndDescription', '$content', 'v2.1')"
            val result = ChutneyServerApiUtils.post<Base>(query, body)
            EventDataLogger.logInfo("Scenario Added to Local Server.<br>", project)
        } catch (e: Exception) {
            LOG.debug(e.toString())
            EventDataLogger.logError(e.toString(), project)
        }

    }

}

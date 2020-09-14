package com.chutneytesting.idea.vcs

import com.chutneytesting.idea.ChutneyUtil
import com.chutneytesting.idea.ChutneyUtil.getChutneyScenarioIdFromFileName
import com.chutneytesting.idea.ChutneyUtil.isChutneyFragmentsJson
import com.chutneytesting.idea.ChutneyUtil.isChutneyJson
import com.chutneytesting.idea.ChutneyUtil.isRemoteChutneyJson
import com.chutneytesting.idea.actions.converter.ScenarioV1ToV2Converter
import com.chutneytesting.idea.logger.EventDataLogger
import com.chutneytesting.idea.util.ChutneyServerApiUtils
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.NotificationListener
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.checkin.CheckinHandler
import com.intellij.openapi.vcs.checkin.CheckinMetaHandler
import com.intellij.openapi.vcs.ui.RefreshableOnComponent
import com.intellij.psi.PsiManager
import com.intellij.psi.search.searches.ReferencesSearch
import org.apache.commons.lang.StringEscapeUtils.escapeSql
import org.hjson.JsonValue
import org.hjson.Stringify
import java.awt.GridLayout
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel

class ChutneyCheckinHandler(private val checkinProjectPanel: CheckinProjectPanel) : CheckinHandler(),
    CheckinMetaHandler {

    var updateChutneyScenarios: JCheckBox = JCheckBox("Update Remote Chutney Scenarios")

    override fun runCheckinHandlers(finishAction: Runnable) {
        if (updateChutneyScenarios.isSelected) {
            val project = checkinProjectPanel.project
            val psiScenariosToUpdateBecauseOfIceFragUsage = checkinProjectPanel.virtualFiles
                .filter { isChutneyFragmentsJson(it) }
                .mapNotNull { PsiManager.getInstance(project).findFile(it) }
                .map { ReferencesSearch.search(it) }
                .flatMap { it.findAll() }
                .map { it.element.containingFile }
                .filter { isChutneyJson(it) }
                .distinct()
                .toList()

            val psiScenariosModified = checkinProjectPanel.virtualFiles
                .filter { isChutneyJson(it) }
                .mapNotNull { PsiManager.getInstance(project).findFile(it) }
                .toList()

            (psiScenariosToUpdateBecauseOfIceFragUsage + psiScenariosModified)
                .filter { isRemoteChutneyJson(it) }
                .distinctBy { it.virtualFile.path }
                .forEach {
                    val query = ChutneyServerApiUtils.getRemoteDatabaseUrl()
                    val processJsonReference = ChutneyUtil.processJsonReference(it.virtualFile)
                    val convert = ScenarioV1ToV2Converter().convert(processJsonReference)
                    val hJsonString = JsonValue.readHjson(convert).toString(Stringify.PLAIN)
                    val id = getChutneyScenarioIdFromFileName(it.name)
                    val escapeSql = escapeSql(hJsonString)
                    val body = "update scenario set content='$escapeSql', version='v2.1' where id = '$id'"
                    try {
                        ChutneyServerApiUtils.post<Any>(query, body)
                        EventDataLogger.logInfo(
                            "Remote scenario files updated with success.<br>" +
                                    "<a href=\"${ChutneyServerApiUtils.getRemoteServerUrl()}/#/scenario/\">Open Chutney Server</a>",
                            project,
                            NotificationListener.URL_OPENING_LISTENER
                        )

                    } catch (e: Exception) {
                        EventDataLogger.logError(e.toString(), project)
                    }
                }
        }
        finishAction.run()
    }


    override fun getBeforeCheckinConfigurationPanel(): RefreshableOnComponent? {
        return object : RefreshableOnComponent {

            override fun getComponent(): JComponent {
                val panel = JPanel(GridLayout(1, 0))
                panel.add(updateChutneyScenarios)
                return panel
            }

            override fun restoreState() {
                updateChutneyScenarios.isSelected = PropertiesComponent.getInstance(checkinProjectPanel.project)
                    .getBoolean("updateChutneyScenarios")
            }

            override fun saveState() {
                PropertiesComponent.getInstance(checkinProjectPanel.project)
                    .setValue("updateChutneyScenarios", updateChutneyScenarios.isSelected)
            }

            override fun refresh() {
            }

        }
    }
}

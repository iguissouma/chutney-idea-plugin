package com.chutneytesting.idea.actions

import com.chutneytesting.idea.logger.EventDataLogger
import com.chutneytesting.idea.util.ChutneyServerApiUtils
import com.intellij.notification.NotificationListener
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages
import javax.swing.Icon


class AddRemoveScenarioInCampaignAction(
    val campaignId: Int,
    val scenarioId: Int,
    val selected: Boolean,
    text: String?,
    description: String?,
    icon: Icon?
) : AnAction(text, description, icon) {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        try {
            val query: String = if (selected) {
                //remove from campaign
                "delete from campaign_scenarios where campaign_id = '$campaignId' and scenario_id = '$scenarioId'"
            } else {
                //add to campaign
                "insert into campaign_scenarios (campaign_id, scenario_id) values('$campaignId','$scenarioId')"
            }
            val post = ChutneyServerApiUtils.post<Base>(ChutneyServerApiUtils.getRemoteDatabaseUrl(), query)

            EventDataLogger.logInfo(
                "scenario" + (if (selected) " removed from" else " added to") + " campaign with success.<br>" +
                        "<a href=\"${ChutneyServerApiUtils.getRemoteServerUrl()}/#/campaign/$campaignId/execution\">Open Campaign in remote Chutney Server</a>",
                project,
                NotificationListener.URL_OPENING_LISTENER
            )
        } catch (e: Exception) {
            EventDataLogger.logError(e.toString(), project)
        }


    }

}

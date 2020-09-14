package com.chutneytesting.idea.actions

import com.chutneytesting.idea.ChutneyUtil
import com.chutneytesting.idea.util.ChutneyServerApiUtils
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*

data class Campaign(val id: Int, val label: String)

class DynamicActionGroup : ActionGroup() {

    val campaigns by lazy {
        val query = ChutneyServerApiUtils.getRemoteDatabaseUrl()
        val campaignsQuery = "(select id, title from campaign order by id)"
        val post = ChutneyServerApiUtils.post<Base>(query, campaignsQuery)
        post.table?.rows?.map { Campaign(it.values!![0].toInt(), it.values[1]) } ?: emptyList()
    }

    fun inCampaigns(id: Int): List<Int> {
        val inCampaignsQuery = "(select campaign_id from campaign_scenarios where scenario_id = '$id')"
        val post = ChutneyServerApiUtils.post<Base>(ChutneyServerApiUtils.getRemoteDatabaseUrl(), inCampaignsQuery)
        return post.table?.rows?.map { it.values!![0].toInt() }?.toList() ?: emptyList()
    }

    override fun getChildren(event: AnActionEvent?): Array<out AnAction> {
        val file = event?.getData(DataKeys.VIRTUAL_FILE) ?: return emptyArray()
        val id = ChutneyUtil.getChutneyScenarioIdFromFileName(file.name)
        val inCampaigns = inCampaigns(id!!)
        return campaigns.map {
            val selected = inCampaigns.contains(it.id)
            AddRemoveScenarioInCampaignAction(
                it.id,
                id,
                selected,
                it.id.toString() + "-" + it.label,
                it.label,
                if (selected) AllIcons.Actions.Checked_selected else null
            )
        }.toTypedArray()
    }

    override fun update(event: AnActionEvent) {
        // Enable/disable depending
        val project = event.project
        val virtualFile = event.getData(DataKeys.VIRTUAL_FILE)
        val psiFile = event.getData(LangDataKeys.PSI_FILE)
        event.presentation.isEnabledAndVisible =
            project != null && psiFile != null && virtualFile != null && !virtualFile.isDirectory &&
                    ChutneyUtil.isRemoteChutneyJson(psiFile)
    }
}

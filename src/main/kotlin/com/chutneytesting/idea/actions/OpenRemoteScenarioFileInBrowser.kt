package com.chutneytesting.idea.actions

import com.chutneytesting.idea.ChutneyUtil.getChutneyScenarioIdFromFileName
import com.chutneytesting.idea.util.ChutneyServerApiUtils.checkRemoteServerUrlConfig
import com.chutneytesting.idea.util.ChutneyServerApiUtils.getRemoteServerUrl
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataKeys

class OpenRemoteScenarioFileInBrowser : RemoteScenarioBaseAction() {


    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        if (!checkRemoteServerUrlConfig(project)) return
        val file = event.getData(DataKeys.VIRTUAL_FILE) ?: return
        val id = getChutneyScenarioIdFromFileName(file.name) ?: return
        BrowserUtil.browse("${getRemoteServerUrl()}/#/scenario/$id/execution/last")
    }

}

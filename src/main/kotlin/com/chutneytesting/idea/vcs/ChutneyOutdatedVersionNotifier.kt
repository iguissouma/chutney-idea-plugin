package com.chutneytesting.idea.vcs

import com.chutneytesting.idea.ChutneyIcons
import com.chutneytesting.idea.ChutneyUtil
import com.chutneytesting.idea.actions.Base
import com.chutneytesting.idea.actions.converter.ScenarioV2ToV1Converter
import com.chutneytesting.idea.util.ChutneyServerApiUtils
import com.chutneytesting.idea.util.HJsonUtils
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications


class ChutneyOutdatedVersionNotifier(val project: Project) : EditorNotifications.Provider<EditorNotificationPanel>(),
    DumbAware {

    private val KEY =
        Key.create<EditorNotificationPanel>("outdated.chutney.scenario.source.file.editing.notification.panel")

    override fun getKey(): Key<EditorNotificationPanel> {
        return KEY
    }

    override fun createNotificationPanel(virtualFile: VirtualFile, fileEditor: FileEditor): EditorNotificationPanel? {
        val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return null
        if (!ChutneyUtil.isChutneyJson(psiFile)) return null
        val id = ChutneyUtil.getChutneyScenarioIdFromFileName(virtualFile.name) ?: return null
        val query: String = ChutneyServerApiUtils.getRemoteDatabaseUrl()
        val body = "(select * from scenario where id = '$id')"
        val result = ChutneyServerApiUtils.post<Base>(query, body)
        result.table?.rows?.forEach {
            val version = it.values?.get(8)
            val rawScenario = it.values?.get(3)!!
            var convertedScenario = rawScenario
            if (ChutneyUtil.isChutneyV1Json(psiFile) && version?.matches(Regex("v2.*"))!!) {
                convertedScenario = ScenarioV2ToV1Converter().convert(rawScenario)
            }
            val replace = HJsonUtils.convertHjson(convertedScenario).replace("\r\n", "\n")
            if (replace != HJsonUtils.convertHjson(psiFile.text)) {
                val panel = EditorNotificationPanel()
                panel.icon(ChutneyIcons.ChutneyToolWindow)
                panel.createActionLabel("Show Diff", "Chutney.ShowDiffBetweenLocalScenarioFileAndRemote")
                panel.createActionLabel("Update Local Scenario", "Chutney.UpdateLocalScenarioFromRemoteServer")
                panel.createActionLabel("Update Remote Scenario", "Chutney.UpdateRemoteScenarioFromLocal")
                panel.setText("Outdated version.")
                return panel
            }
        }
        return null
    }


}

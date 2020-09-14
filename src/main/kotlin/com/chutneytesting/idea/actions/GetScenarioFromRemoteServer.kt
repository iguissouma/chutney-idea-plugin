package com.chutneytesting.idea.actions

import com.chutneytesting.idea.actions.converter.ScenarioV2ToV1Converter
import com.chutneytesting.idea.actions.ui.ValueLabelComboBox
import com.chutneytesting.idea.logger.EventDataLogger
import com.chutneytesting.idea.util.ChutneyServerApiUtils
import com.chutneytesting.idea.util.ChutneyServerApiUtils.checkRemoteServerUrlConfig
import com.chutneytesting.idea.util.ChutneyServerApiUtils.getRemoteServerUrl
import com.chutneytesting.idea.util.HJsonUtils
import com.chutneytesting.idea.util.sanitizeFilename
import com.google.gson.Gson
import com.intellij.json.JsonFileType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.components.JBCheckBox
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import java.nio.file.Paths
import javax.swing.JPanel

data class ValueLabel(val value: String, val label: String)

class GetScenarioFromRemoteServer : AnAction() {

    private val LOG = Logger.getInstance(GetScenarioFromRemoteServer::class.java)

    override fun actionPerformed(event: AnActionEvent) {
        val remoteServerUrl = getRemoteServerUrl()
        val project = event.project ?: return
        if (!checkRemoteServerUrlConfig(project)) return
        val query = ChutneyServerApiUtils.getRemoteDatabaseUrl()
        val nav = event.getData(CommonDataKeys.NAVIGATABLE) ?: return
        val directory: PsiDirectory? = if (nav is PsiDirectory) nav else (nav as PsiFile).parent

        val dialogBuilder = DialogBuilder().title("Select Scenario")
        val centerPanel = JPanel(BorderLayout())

        val campaignsQuery = "(select id, title from scenario order by id)"
        val post = ChutneyServerApiUtils.post<Base>(query, campaignsQuery)
        val items = mutableListOf<ValueLabel>()
        post.table?.rows?.forEach {
            val valueLabel = ValueLabel(it.values!![0], it.values[1])
            items.add(valueLabel)
        }
        val myComboBox = ValueLabelComboBox(CollectionComboBoxModel(items))

        val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
        descriptor.title = "Select Directory"
        val textFieldWithBrowseButton = TextFieldWithBrowseButton()
        textFieldWithBrowseButton.text = directory?.virtualFile?.canonicalPath ?: ""
        textFieldWithBrowseButton.addBrowseFolderListener("Select directory", "Select directory", project, descriptor)

        val box = JBCheckBox("Replace variables in scenario", true)
        val panel = FormBuilder.createFormBuilder().addComponent(myComboBox)
            .addComponent(textFieldWithBrowseButton)
            .addComponent(box)
            .panel

        centerPanel.add(panel)
        dialogBuilder.centerPanel(centerPanel).resizable(false)


        dialogBuilder.setOkOperation {
            dialogBuilder.dialogWrapper.close(DialogWrapper.OK_EXIT_CODE)
            val selectedItem = myComboBox.selectedItem as ValueLabel
            val idScenario = selectedItem.value
            val body = "(select * from scenario where id='$idScenario')"
            try {
                val result = ChutneyServerApiUtils.post<Base>(query, body)
                val psiFileFactory = PsiFileFactory.getInstance(project)
                result.table?.rows?.forEach {
                    var convertedScenario = it.values?.get(3)!!
                    if (it.values[8].matches(Regex("v2.*"))) {
                        convertedScenario = ScenarioV2ToV1Converter().convert(convertedScenario)
                    }
                    var convertHJson = HJsonUtils.convertHjson(convertedScenario)
                    if (box.isSelected) {
                        try {
                            val variables: Map<String, String> =
                                Gson().fromJson(it.values[6], mutableMapOf<String, String>().javaClass)
                            variables.keys.forEach { key ->
                                convertHJson = convertHJson.replace("**$key**", variables.getValue(key), false)
                            }
                        } catch (e: Exception) {
                            // ignore exception
                        }
                    }
                    val psiFileCreated = psiFileFactory.createFileFromText(
                        sanitizeFilename(it.values[0]) + "-" + it.values[1].toLowerCase().trim() + ".chutney." + JsonFileType.INSTANCE.defaultExtension,
                        JsonFileType.INSTANCE,
                        convertHJson
                    )
                    val scenarioFile = CodeStyleManager.getInstance(project).reformat(psiFileCreated)
                    val targetDirectory = VfsUtil.findFile(Paths.get(textFieldWithBrowseButton.text), true)
                        ?.let { PsiManager.getInstance(project).findDirectory(it) }
                    targetDirectory?.add(scenarioFile)
                }

            } catch (e: Exception) {
                EventDataLogger.logError(e.toString(), project)
            }
        }

        dialogBuilder.show()
    }

}

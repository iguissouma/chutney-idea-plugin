package com.chutneytesting.idea.settings

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class ChutneySettingsConfigurable(private var chutneySettings: ChutneySettings) : Configurable {

    val remoteServerField: JBTextField = JBTextField()

    fun ChutneySettingsConfigurable(chutneySettings: ChutneySettings) {
        this.chutneySettings = chutneySettings
    }

    override fun isModified(): Boolean {
        return remoteServerField.text != chutneySettings.getRemoteServerUrl() ?: false
    }

    override fun getDisplayName(): String {
        return "Chutney"
    }

    override fun apply() {
        this.chutneySettings.setRemoteServerUrl(remoteServerField.text)
    }

    override fun createComponent(): JComponent? {
        remoteServerField.text = ChutneySettings.getInstance().getRemoteServerUrl() ?: ""

        val myWrapper = JPanel(BorderLayout())
        val centerPanel =
            FormBuilder.createFormBuilder().addLabeledComponent("Remote Server: ", remoteServerField).panel
        myWrapper.add(centerPanel, BorderLayout.NORTH)

        return myWrapper
    }

    companion object {
        fun getInstance(): ChutneySettingsConfigurable {
            return ServiceManager.getService(ChutneySettingsConfigurable::class.java)
        }
    }
}

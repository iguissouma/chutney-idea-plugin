package com.chutneytesting.idea.settings

import com.chutneytesting.idea.actions.Base
import com.chutneytesting.idea.util.ChutneyServerApiUtils
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import java.awt.Color
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

class ChutneySettingsConfigurable(private var chutneySettings: ChutneySettings) : Configurable {

    val remoteServerField: JBTextField = JBTextField()
    val remoteUserField: JBTextField = JBTextField()
    val remotePasswordField: JBPasswordField = JBPasswordField()


    fun ChutneySettingsConfigurable(chutneySettings: ChutneySettings) {
        this.chutneySettings = chutneySettings
    }

    override fun isModified(): Boolean {
        return remoteServerField.text != chutneySettings.getRemoteServerUrl()
                || remoteUserField.text != chutneySettings.getRemoteUser()
                || remotePasswordField.text != chutneySettings.getRemotePassword() ?: false
    }

    override fun getDisplayName(): String {
        return "Chutney"
    }

    override fun apply() {
        this.chutneySettings.setRemoteServerUrl(remoteServerField.text)
        this.chutneySettings.setRemoteUser(remoteUserField.text)
        this.chutneySettings.setRemotePassword(remotePasswordField.text)
    }

    override fun createComponent(): JComponent? {
        val settingsInstance = ChutneySettings.getInstance()
        remoteServerField.text = settingsInstance.getRemoteServerUrl() ?: ""
        remoteUserField.text = settingsInstance.getRemoteUser() ?: ""
        remotePasswordField.text = settingsInstance.getRemotePassword() ?: ""

        val checkConnectionButton = JButton("Check connection")
        val checkLabel = JBLabel("").apply { isVisible = false }

        checkConnectionButton.addActionListener {
            try {
                ChutneyServerApiUtils.post<Base>(ChutneyServerApiUtils.getRemoteDatabaseUrl(), "(select 1 from campaign)")
                checkLabel.text = "Connection successfull"
                checkLabel.foreground = Color.GREEN
            } catch (exception: Exception) {
                checkLabel.text = "Connection failed"
                checkLabel.foreground = Color.RED
            }
            checkLabel.isVisible = true
        }


        val myWrapper = JPanel(BorderLayout())
        val centerPanel =
                FormBuilder.createFormBuilder()
                        .addLabeledComponent("Remote Server: ", remoteServerField)
                        .addLabeledComponent("User: ", remoteUserField)
                        .addLabeledComponent("Password: ", remotePasswordField)
                        .addComponent(checkConnectionButton)
                        .addComponent(checkLabel)
                        .panel
        myWrapper.add(centerPanel, BorderLayout.NORTH)

        return myWrapper
    }

    companion object {
        fun getInstance(): ChutneySettingsConfigurable {
            return ServiceManager.getService(ChutneySettingsConfigurable::class.java)
        }
    }
}

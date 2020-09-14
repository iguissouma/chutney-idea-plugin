package com.chutneytesting.idea.runner

import com.chutneytesting.idea.ChutneyIcons
import com.intellij.execution.configurations.*
import com.intellij.openapi.project.Project


class ChutneyRunConfigurationType
    : ConfigurationTypeBase("Chutney-scenario-runner", "Chutney", "Chutney scenario runner", ChutneyIcons.ChutneyAction) {

    init {
        this.addFactory(object : ConfigurationFactory(this as ConfigurationType) {

            override fun createTemplateConfiguration(project: Project): RunConfiguration {
                return ChutneyRunConfiguration(project, this, "Chutney")
            }

            override fun isConfigurationSingletonByDefault(): Boolean {
                return true
            }

        })
    }

    companion object {
        fun getInstance(): ChutneyRunConfigurationType {
            return ConfigurationTypeUtil.findConfigurationType(ChutneyRunConfigurationType::class.java)
        }
    }
}

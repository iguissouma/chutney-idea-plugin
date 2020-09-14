package com.chutneytesting.idea.template

import com.intellij.codeInsight.template.impl.DefaultLiveTemplatesProvider

class ChutneyLiveTemplateDefaultProvider : DefaultLiveTemplatesProvider {

    override fun getHiddenLiveTemplateFiles(): Array<String>? {
        return emptyArray()
    }

    override fun getDefaultLiveTemplateFiles(): Array<String> {
        return arrayOf("/liveTemplates/Chutney")
    }

}

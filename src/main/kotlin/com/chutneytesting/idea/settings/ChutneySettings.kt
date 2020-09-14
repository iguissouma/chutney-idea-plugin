package com.chutneytesting.idea.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(name = "ChutneySettings", storages = arrayOf(Storage("chutney.xml")))
class ChutneySettings : PersistentStateComponent<ChutneySettings.State> {

    private val myState = State()

    override fun getState(): State? {
        return myState
    }

    override fun loadState(state: State) {
        XmlSerializerUtil.copyBean(state, myState)
    }

    fun setRemoteServerUrl(remoteServerUrl: String) {
        this.myState.remoteServerUrl = remoteServerUrl
    }

    fun getRemoteServerUrl(): String? {
        return this.myState.remoteServerUrl
    }

    class State {
        var remoteServerUrl: String? = ""
    }

    companion object {
        fun getInstance(): ChutneySettings {
            return ServiceManager.getService(ChutneySettings::class.java)
        }
    }

}

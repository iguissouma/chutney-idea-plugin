package com.chutneytesting.idea.actions.ui

import com.chutneytesting.idea.actions.ValueLabel
import com.intellij.ide.ui.UISettings
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.ComboboxSpeedSearch
import com.intellij.ui.ListCellRendererWrapper
import java.awt.event.KeyEvent
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.ListCellRenderer

class ValueLabelComboBox(model: CollectionComboBoxModel<ValueLabel>) : ComboBox<ValueLabel>(model) {

    init {
        object : ComboboxSpeedSearch(this) {
            override fun getElementText(element: Any?): String? {
                return if (element is ValueLabel) element.label else null
            }
        }

        registerUpDownHint(this)

    }

    override fun getRenderer(): ListCellRenderer<in ValueLabel> = object : ListCellRendererWrapper<ValueLabel>() {
        override fun customize(
            list: JList<*>,
            valueLabel: ValueLabel?,
            index: Int,
            selected: Boolean,
            hasFocus: Boolean
        ) {
            if (valueLabel != null) {
                setText("""${valueLabel.value} - ${valueLabel.label}""")
            }
        }
    }

    private fun scrollBy(delta: Int) {
        val size = this.model.size
        if (delta == 0 || size == 0) return
        var next = this.selectedIndex + delta
        if (next < 0 || next >= size) {
            if (!UISettings.instance.cycleScrolling) {
                return
            }
            next = (next + size) % size
        }
        this.selectedIndex = next
    }

    private fun registerUpDownHint(component: JComponent) {
        DumbAwareAction.create { e ->
            if (e.inputEvent is KeyEvent) {
                val code = (e.inputEvent as KeyEvent).keyCode
                scrollBy(if (code == KeyEvent.VK_DOWN) 1 else if (code == KeyEvent.VK_UP) -1 else 0)
            }
        }.registerCustomShortcutSet(CustomShortcutSet(KeyEvent.VK_UP, KeyEvent.VK_DOWN), component)
    }


}

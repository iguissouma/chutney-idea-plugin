package com.chutneytesting.idea.gutter

import com.chutneytesting.idea.ChutneyUtil
import com.chutneytesting.idea.logger.EventDataLogger
import com.chutneytesting.idea.runner.ChutneyKotlinJsr223JvmLocalScriptEngineFactory
import com.chutneytesting.idea.runner.getFullyQualifiedMethodName
import com.chutneytesting.idea.runner.moduleIsUpToDate
import com.chutneytesting.idea.util.ChutneyServerApiUtils
import com.chutneytesting.idea.util.HJsonUtils
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.notification.NotificationListener
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.IconLoader
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.task.ProjectTaskManager
import com.intellij.ui.EditorNotifications
import com.intellij.ui.ListCellRendererWrapper
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.UIUtil
import org.apache.commons.lang.StringEscapeUtils
import org.jetbrains.kotlin.psi.KtNamedFunction
import java.awt.event.MouseEvent
import java.util.concurrent.TimeUnit
import javax.swing.JList

val icon = IconLoader.getIcon("icons/ksync.svg")

class ChutneyKotlinSynchroniseWithRemoteLineMarkerProvider : LineMarkerProvider{

    override fun getLineMarkerInfo(psiElement: PsiElement): LineMarkerInfo<*>? {
        if (psiElement is KtNamedFunction && ChutneyUtil.isChutneyDslMethod(psiElement)) {
            val displayName = "${psiElement.name}"
            return lineMarkerInfo(psiElement.funKeyword!!, displayName)
        }
        return null
    }


    override fun collectSlowLineMarkers(
        elements: MutableList<out PsiElement>,
        result:  MutableCollection<in LineMarkerInfo<*>>
    ) {}

    protected fun lineMarkerInfo(anchor: PsiElement, displayName: String): LineMarkerInfo<PsiElement> {
        return LineMarkerInfo(
            anchor,
            anchor.textRange,
            icon,
            { e -> e.containingFile.name },
            { e, elt ->
                run {
                    if (elt.isValid) {
                        showPopup(e, elt, displayName)
                    }
                }

            }
            , GutterIconRenderer.Alignment.RIGHT
        )
    }

    private fun showPopup(e: MouseEvent, psiElement: PsiElement, displayName: String) {
        JBPopupFactory.getInstance()
            .createPopupChooserBuilder(listOf("synchronise"))
            .setRenderer(object : ListCellRendererWrapper<String>() {
                override fun customize(p0: JList<*>?, value: String?, index: Int, selected: Boolean, hasFocus: Boolean) {
                    setIcon(icon)
                    setText("$value $displayName")
                }
            })
            .setMovable(true)
            .setItemChosenCallback { type ->
                if (psiElement.isValid) {
                    executeSyncRemote(psiElement)
                }
            }.createPopup().show(RelativePoint(e))
    }

    private fun executeSyncRemote(psiElement: PsiElement) {
        val virtualFile = psiElement.containingFile.virtualFile ?: error("cannot find virtualFile")
        val project = psiElement.project
        if (!ChutneyServerApiUtils.checkRemoteServerUrlConfig(project)) return
        val module = ModuleUtil.findModuleForFile(virtualFile, project) ?: error("cannot find module")

        if (moduleIsUpToDate(module).not()) {
            ProjectTaskManager.getInstance(project).build(module).blockingGet(60, TimeUnit.SECONDS)
        }

        val script = """
                    import com.chutneytesting.kotlin.dsl.*    
                     
                    ${getFullyQualifiedMethodName(psiElement)}()
                    """.trimIndent()

        val eval =
            ChutneyKotlinJsr223JvmLocalScriptEngineFactory(virtualFile, project).scriptEngine.eval(script)

        val scenariosToUpdate = if (eval is List<*>) eval else listOf(eval)
        val query = ChutneyServerApiUtils.getRemoteDatabaseUrl()

        scenariosToUpdate.filterNotNull().forEach { scenario ->
            val id = scenario::class.members.firstOrNull { it.name == "id" }?.call(scenario) ?: return@forEach
            val title = scenario::class.members.firstOrNull { it.name == "title" }?.call(scenario) ?: return@forEach
            val escapeSql = StringEscapeUtils.escapeSql("$scenario")
            val body = "update scenario set content='$escapeSql', version=2.1 where id = '$id'"
            try {
                FilenameIndex.getFilesByName(
                    project,
                    "$id-${title}.chutney.json",
                    GlobalSearchScope.moduleScope(module)
                ).forEachIndexed { index, psiFile  ->
                    val document = PsiDocumentManager.getInstance(project).getDocument(psiFile) ?: return@forEachIndexed
                    document.setText("$scenario".replace("\r\n", "\n"))
                    FileDocumentManager.getInstance().saveDocument(document)
                    UIUtil.invokeAndWaitIfNeeded(Runnable {
                        CodeStyleManager.getInstance(project).reformat(psiFile)
                    })
                }
                val post = ChutneyServerApiUtils.post<Map<String, Any?>>(query, body)
                if (post["updatedRows"] as? Double == 1.toDouble()) {
                    EventDataLogger.logInfo(
                        "Remote scenario file updated with success.<br>" +
                                "<a href=\"${ChutneyServerApiUtils.getRemoteServerUrl()}/#/scenario/$id/execution/last\">Open in remote Chutney Server</a>",
                        project,
                        NotificationListener.URL_OPENING_LISTENER
                    )
                } else {
                    EventDataLogger.logError("Remote scenario file could not be updated.<br> cause: [${post["error"]}]", project)
                }
            } catch (e: Exception) {
                EventDataLogger.logError(e.toString(), project)
            }
        }
    }

}
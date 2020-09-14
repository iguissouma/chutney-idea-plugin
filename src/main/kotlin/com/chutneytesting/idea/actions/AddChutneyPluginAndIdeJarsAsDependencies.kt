package com.chutneytesting.idea.actions;

import addLibraryDependencyTo
import com.chutneytesting.dsl.ChutneyPluginPaths
import com.chutneytesting.idea.ChutneyUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType.CLASSES
import removeLibraryDependencyFrom
import java.io.File

class AddChutneyPluginAndIdeJarsAsDependencies : AnAction(), DumbAware {

    private val livePluginAndIdeJarsLibrary = "ChutneyPlugin and IDE jars (to enable navigation and auto-complete)"
    private val projectLibrariesNames = ProjectLibrariesNames()
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        if (projectLibrariesNames.contains(project, livePluginAndIdeJarsLibrary)) {
            removeLibraryDependencyFrom(project, livePluginAndIdeJarsLibrary)
        } else {
            // val livePluginSrc = Pair("jar://" + PathUtil.getJarPathForClass(LivePluginAppComponent::class.java) + "!/", OrderRootType.SOURCES)
            val livePluginJars =
                (File(ChutneyPluginPaths.chutneyIdeaPluginLibPath).listFiles()
                    ?.filter { it.name.endsWith(".jar") }
                    ?.map { Pair("jar://${it.absolutePath}!/", CLASSES) } ?: emptyList())

            /*  val ideJars = File(ChutneyPluginPaths.ideJarsPath).listFiles()
                      ?.filter { it.name.endsWith(".jar") }
                      ?.map { Pair("jar://${it.absolutePath}!/", CLASSES) } ?: emptyList() */
            addLibraryDependencyTo(project, livePluginAndIdeJarsLibrary, livePluginJars) // + livePluginSrc + ideJars)
        }
    }

    override fun update(event: AnActionEvent) {
        val project = event.project ?: return
        val psiFile = event.getData(LangDataKeys.PSI_FILE) ?: return
        event.presentation.isEnabledAndVisible = ChutneyUtil.isChutneyDsl(psiFile)
        if (projectLibrariesNames.contains(project, livePluginAndIdeJarsLibrary)) {
            event.presentation.text = "Remove ChutneyPlugin and IDE Jars from Project"
            event.presentation.description = "" +
                    "Remove ChutneyPlugin and IDE Jars from project dependencies. " +
                    "This will enable auto-complete and other IDE features for IntelliJ classes."
        } else {
            event.presentation.text = "Add ChutneyPlugin and IDE Jars to Project"
            event.presentation.description = "" +
                    "Add ChutneyPlugin and IDE jars to project dependencies. " +
                    "This will enable auto-complete and other IDE features."
        }
    }

    private class ProjectLibrariesNames {
        private var modificationCount = -1L
        private var value = emptyList<String>()

        fun contains(project: Project, libraryName: String): Boolean {
            val moduleManager = ModuleManager.getInstance(project)
            if (moduleManager.modificationCount != modificationCount) {
                value = moduleManager.modules.flatMap { module ->
                    val moduleRootManager = ModuleRootManager.getInstance(module).modifiableModel
                    try {
                        moduleRootManager.moduleLibraryTable.libraries.mapNotNull { it.name }
                    } finally {
                        moduleRootManager.dispose()
                    }
                }
                modificationCount = moduleManager.modificationCount
            }
            return value.contains(libraryName)
        }
    }

}

package com.chutneytesting.idea.logger

import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project

object EventDataLogger {

    fun logError(htmlMessage: String, project: Project, notificationListener: NotificationListener? = null) {
        val notification = Notification(
            "Chutney Notifications",
            "Chutney error",
            htmlMessage,
            NotificationType.ERROR,
            notificationListener
        )
        Notifications.Bus.notify(notification, project)
    }

    fun logWarning(htmlMessage: String, project: Project, notificationListener: NotificationListener? = null) {
        val notification = Notification(
            "Chutney Notifications",
            "Chutney warning",
            htmlMessage,
            NotificationType.WARNING,
            notificationListener
        )
        Notifications.Bus.notify(notification, project)
    }

    fun logInfo(htmlMessage: String, project: Project, notificationListener: NotificationListener? = null) {
        val notification = Notification(
            "Chutney Notifications",
            "Chutney info",
            htmlMessage,
            NotificationType.INFORMATION,
            notificationListener
        )
        Notifications.Bus.notify(notification, project)
    }
}

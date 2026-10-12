package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import java.util.concurrent.atomic.AtomicInteger

class CherryNotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_MESSAGES_ID = "channel_ai_messages"
        const val CHANNEL_SOCIAL_ID = "channel_ai_social"
        const val CHANNEL_ANNOUNCEMENTS_ID = "channel_ai_announcements"
    }

    private val notificationIdCounter = AtomicInteger(100)

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val messagesChannel = NotificationChannel(
                CHANNEL_MESSAGES_ID,
                context.getString(R.string.channel_messages_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_messages_desc)
                enableVibration(true)
            }

            val socialChannel = NotificationChannel(
                CHANNEL_SOCIAL_ID,
                context.getString(R.string.channel_social_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.channel_social_desc)
                enableVibration(true)
            }

            val announcementsChannel = NotificationChannel(
                CHANNEL_ANNOUNCEMENTS_ID,
                context.getString(R.string.channel_announcements_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.channel_announcements_desc)
            }

            notificationManager.createNotificationChannels(
                listOf(messagesChannel, socialChannel, announcementsChannel)
            )
        }
    }

    private fun canPostNotifications(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showAiMessageNotification(title: String, message: String) {
        if (!canPostNotifications()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "chat")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationIdCounter.incrementAndGet(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_MESSAGES_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationIdCounter.incrementAndGet(), notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun showFriendRequestNotification(senderName: String, info: String) {
        if (!canPostNotifications()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "notifications")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationIdCounter.incrementAndGet(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_SOCIAL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("New Friend Request: $senderName")
            .setContentText(info)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$senderName wants to connect and share AI prompts: $info"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationIdCounter.incrementAndGet(), notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun showAnnouncementNotification(title: String, message: String) {
        if (!canPostNotifications()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "notifications")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationIdCounter.incrementAndGet(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ANNOUNCEMENTS_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("📢 $title")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationIdCounter.incrementAndGet(), notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}

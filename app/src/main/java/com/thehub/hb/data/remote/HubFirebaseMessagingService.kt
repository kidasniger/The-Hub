package com.thehub.hb.data.remote

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.RemoteMessage
import com.thehub.hb.MainActivity
import com.thehub.hb.R
import com.thehub.hb.data.repository.MessageRepository
import com.thehub.hb.data.repository.NotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class HubFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID = "hub_social_notifications"
        private const val CHANNEL_NAME = "Notifications sociales"
        private const val CHANNEL_DESCRIPTION =
            "Notifications pour les likes, commentaires, abonnements et messages."
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        serviceScope.launch {
            NotificationRepository().registerCurrentFcmToken()
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data
        val notificationPayload = message.notification

        val conversationId = data["conversationId"].orEmpty()
        val messageId = data["messageId"].orEmpty()
        if (conversationId.isNotBlank() && messageId.isNotBlank()) {
            serviceScope.launch {
                MessageRepository().markDelivered(conversationId, listOf(messageId))
            }
        }

        if (data.isEmpty() && notificationPayload == null) return

        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
            ?: return

        serviceScope.launch {
            val recipientId = data["recipientId"]?.takeIf { it.isNotBlank() }
            if (recipientId != null) {
                if (recipientId != currentUid) {
                    return@launch
                }
            } else {
                // Backward compatibility with pushes emitted by the previously
                // deployed Cloud Function: verify ownership through Firestore.
                val notificationId = data["notificationId"].orEmpty()
                if (notificationId.isBlank() || !isNotificationForCurrentUser(
                        notificationId = notificationId,
                        currentUid = currentUid
                    )
                ) {
                    return@launch
                }
            }

            var actorDisplayName = data["actorDisplayName"]?.takeIf { it.isNotBlank() }
            if (actorDisplayName == null) {
                val actorId = data["actorId"].orEmpty()
                actorDisplayName = resolveActorDisplayName(
                    actorId = actorId,
                    fallback = data["actorUsername"]?.takeIf { it.isNotBlank() } ?: "Quelqu'un"
                )
            }

            val title = data["title"]
                ?.takeIf { it.isNotBlank() }
                ?: notificationPayload?.title
                    ?.takeIf { it.isNotBlank() }
                ?: "The Hub"

            val body = data["body"]
                ?.takeIf { it.isNotBlank() }
                ?: notificationPayload?.body
                    ?.takeIf { it.isNotBlank() }
                ?: buildFallbackBody(data, actorDisplayName)

            val deepLink = data["deepLink"]
                ?.takeIf { it.isNotBlank() }
                ?: buildFallbackDeepLink(data)

            val createdAtMs = data["createdAtMs"]?.toLongOrNull()

            showNotification(
                title = title,
                body = body,
                deepLink = deepLink,
                notificationId = message.messageId?.hashCode()
                    ?: (deepLink.hashCode() xor body.hashCode()),
                createdAtMs = createdAtMs
            )
        }
    }

    private suspend fun isNotificationForCurrentUser(
        notificationId: String,
        currentUid: String
    ): Boolean {
        return try {
            val doc = FirebaseFirestore.getInstance()
                .collection("notifications")
                .document(notificationId)
                .get()
                .await()

            doc.exists() && doc.getString("recipientId") == currentUid
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun resolveActorDisplayName(
        actorId: String,
        fallback: String
    ): String {
        if (actorId.isBlank()) return fallback

        return try {
            val doc = FirebaseFirestore.getInstance()
                .collection("users")
                .document(actorId)
                .get()
                .await()

            doc.getString("displayName")
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: doc.getString("name")
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                ?: fallback
        } catch (_: Exception) {
            fallback
        }
    }


    private fun buildFallbackBody(
        data: Map<String, String>,
        actorDisplayName: String? = null
    ): String {
        val actor = actorDisplayName
            ?: data["actorDisplayName"]?.takeIf { it.isNotBlank() }
            ?: data["actorUsername"]?.takeIf { it.isNotBlank() }
            ?: "Quelqu'un"
        return when (data["type"]) {
            "like" -> actor + " a aimé votre publication"
            "comment" -> actor + " a commenté votre publication"
            "follow" -> actor + " a commencé à vous suivre"
            "message" -> actor + " vous a envoyé un message"
            "like_comment" -> actor + " a aimé votre commentaire"
            "reply_comment" -> actor + " a répondu à votre commentaire"
            else -> "Vous avez une nouvelle notification"
        }
    }

    private fun buildFallbackDeepLink(data: Map<String, String>): String {
        val type = data["type"].orEmpty()
        val actorId = data["actorId"].orEmpty()
        val postId = data["postId"].orEmpty()
        val conversationId = data["conversationId"].orEmpty()

        if (type == "follow" && actorId.isNotBlank()) {
            return "thehub://profile/" + Uri.encode(actorId)
        }

        if (type == "message" && conversationId.isNotBlank()) {
            return "thehub://chat/" + Uri.encode(conversationId)
        }

        if (
            (type == "comment" || type == "like_comment" || type == "reply_comment") &&
            postId.isNotBlank()
        ) {
            return "thehub://comments/" + Uri.encode(postId)
        }

        if (postId.isNotBlank()) {
            return "thehub://post/" + Uri.encode(postId)
        }

        return "thehub://feed"
    }
    private fun showNotification(
        title: String,
        body: String,
        deepLink: String,
        notificationId: Int,
        createdAtMs: Long? = null
    ) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        createNotificationChannel()

        val intent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(deepLink)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_NOTIFICATION_DEEP_LINK, deepLink)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_hub_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setWhen(createdAtMs ?: System.currentTimeMillis())
            .setShowWhen(true)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .build()

        try {
            NotificationManagerCompat.from(this).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Notification permission can be revoked between the permission check and notify().
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHANNEL_DESCRIPTION
        }
        manager.createNotificationChannel(channel)
    }
}

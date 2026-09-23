package com.thehub.hb

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.compose.ui.Modifier
import com.thehub.hb.data.repository.MessageRepository
import com.thehub.hb.navigation.HubNavGraph
import com.thehub.hb.ui.theme.AppThemeMode
import com.thehub.hb.ui.theme.FrenchHubStrings
import com.thehub.hb.ui.theme.HubBackground
import com.thehub.hb.ui.theme.HubTextPrimary
import com.thehub.hb.ui.theme.LocalHubStrings
import com.thehub.hb.ui.theme.TheHubTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_OPEN_UPDATE_DIALOG = "OPEN_UPDATE_DIALOG"
        const val EXTRA_NOTIFICATION_DEEP_LINK = "NOTIFICATION_DEEP_LINK"
        private const val PUSH_PERMISSION_PREFS = "push_permission"
        private const val PUSH_PERMISSION_PROMPTED = "prompted"
    }

    private var openUpdateDialogRequest by mutableStateOf(false)
    private var pendingNotificationDeepLink by mutableStateOf<String?>(null)
    private val presenceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val messageRepository = MessageRepository()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onStart() {
        super.onStart()
        presenceScope.launch {
            if (messageRepository.currentUserId != null) {
                runCatching { messageRepository.setPresence(true) }
            }
        }
    }

    override fun onStop() {
        presenceScope.launch {
            if (messageRepository.currentUserId != null) {
                runCatching { messageRepository.setPresence(false) }
            }
        }
        super.onStop()
    }

    override fun onDestroy() {
        presenceScope.cancel()
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        consumeUpdateIntent(intent)
        consumeNotificationIntent(intent)
        enableEdgeToEdge()

        val appContainer = (application as HubApplication).container

        setContent {
            val currentThemeMode by appContainer.dataStoreManager.appThemeMode.collectAsState(
                initial = AppThemeMode.DARK
            )

            CompositionLocalProvider(LocalHubStrings provides FrenchHubStrings) {
                TheHubTheme(themeMode = currentThemeMode) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = HubBackground,
                        contentColor = HubTextPrimary
                    ) {
                        HubNavGraph(
                            appContainer = appContainer,
                            openUpdateDialogRequest = openUpdateDialogRequest,
                            onUpdateDialogRequestConsumed = {
                                openUpdateDialogRequest = false
                            },
                            pendingNotificationDeepLink = pendingNotificationDeepLink,
                            onNotificationDeepLinkConsumed = {
                                pendingNotificationDeepLink = null
                            },
                            onRequestNotificationPermission = {
                                requestNotificationPermissionIfNeeded()
                            }
                        )
                    }
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeUpdateIntent(intent)
        consumeNotificationIntent(intent)
    }

    private fun consumeUpdateIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_UPDATE_DIALOG, false) == true) {
            openUpdateDialogRequest = true
            intent.removeExtra(EXTRA_OPEN_UPDATE_DIALOG)
        }
    }

    private fun consumeNotificationIntent(intent: Intent?) {
        val deepLink = intent?.getStringExtra(EXTRA_NOTIFICATION_DEEP_LINK)
            ?: intent?.data
                ?.takeIf { it.scheme == "thehub" }
                ?.toString()

        if (!deepLink.isNullOrBlank()) {
            if (deepLink == "thehub://update") {
                openUpdateDialogRequest = true
            } else {
                pendingNotificationDeepLink = deepLink
            }
            intent?.removeExtra(EXTRA_NOTIFICATION_DEEP_LINK)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val permissionGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (permissionGranted) return

        val prefs = getSharedPreferences(PUSH_PERMISSION_PREFS, MODE_PRIVATE)
        if (prefs.getBoolean(PUSH_PERMISSION_PROMPTED, false)) return

        prefs.edit().putBoolean(PUSH_PERMISSION_PROMPTED, true).apply()
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

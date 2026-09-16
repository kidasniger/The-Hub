package com.thehub.hb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thehub.hb.navigation.HubNavGraph
import com.thehub.hb.ui.theme.AppLanguage
import com.thehub.hb.ui.theme.AppThemeMode
import com.thehub.hb.ui.theme.EnHubStrings
import com.thehub.hb.ui.theme.FrHubStrings
import com.thehub.hb.ui.theme.HubSurfaceDark
import com.thehub.hb.ui.theme.LocalHubStrings
import com.thehub.hb.ui.theme.TheHubTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as HubApplication).container

        setContent {
            val currentThemeMode by appContainer.dataStoreManager.appThemeMode
                .collectAsStateWithLifecycle(initialValue = AppThemeMode.DARK)

            val currentLanguage by appContainer.dataStoreManager.appLanguage
                .collectAsStateWithLifecycle(initialValue = AppLanguage.FR)

            val strings = when (currentLanguage) {
                AppLanguage.EN -> EnHubStrings
                AppLanguage.FR -> FrHubStrings
            }

            LaunchedEffect(currentLanguage) {
                try {
                    val locale = Locale(currentLanguage.code)
                    Locale.setDefault(locale)
                    val config = resources.configuration
                    config.setLocale(locale)
                    resources.updateConfiguration(config, resources.displayMetrics)
                } catch (_: Exception) {}
            }

            CompositionLocalProvider(LocalHubStrings provides strings) {
                TheHubTheme(themeMode = currentThemeMode) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = HubSurfaceDark
                    ) {
                        HubNavGraph(appContainer = appContainer)
                    }
                }
            }
        }
    }
}

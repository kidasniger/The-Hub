package com.thehub.hb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.thehub.hb.navigation.HubNavGraph
import com.thehub.hb.ui.theme.AppThemeMode
import com.thehub.hb.ui.theme.FrenchHubStrings
import com.thehub.hb.ui.theme.HubBackground
import com.thehub.hb.ui.theme.HubTextPrimary
import com.thehub.hb.ui.theme.LocalHubStrings
import com.thehub.hb.ui.theme.TheHubTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                        HubNavGraph(appContainer = appContainer)
                    }
                }
            }
        }
    }
}

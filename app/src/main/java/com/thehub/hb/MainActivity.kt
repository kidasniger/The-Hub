package com.thehub.hb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.thehub.hb.navigation.HubNavGraph
import com.thehub.hb.ui.theme.FrenchHubStrings
import com.thehub.hb.ui.theme.HubSurfaceDark
import com.thehub.hb.ui.theme.LocalHubStrings
import com.thehub.hb.ui.theme.TheHubTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as HubApplication).container

        setContent {
            CompositionLocalProvider(LocalHubStrings provides FrenchHubStrings) {
                TheHubTheme {
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

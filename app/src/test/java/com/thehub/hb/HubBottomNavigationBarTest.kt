package com.thehub.hb

import android.os.Build
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.thehub.hb.ui.main.HubBottomNavigationBar
import com.thehub.hb.ui.main.MainTab
import com.thehub.hb.ui.theme.FrenchHubStrings
import com.thehub.hb.ui.theme.LocalHubStrings
import com.thehub.hb.ui.theme.TheHubTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class HubBottomNavigationBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun bottom_navigation_renders_all_main_tabs() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalHubStrings provides FrenchHubStrings) {
                TheHubTheme {
                    HubBottomNavigationBar(
                        selectedTab = MainTab.FEED,
                        onTabSelected = {}
                    )
                }
            }
        }

        MainTab.entries.forEach { tab ->
            composeTestRule.onNodeWithTag(tab.testTag).assertIsDisplayed()
        }

        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/main_bottom_navigation.png"
        )
    }

    @Test
    fun bottom_navigation_dispatches_tab_selection() {
        var selectedTab: MainTab? = null

        composeTestRule.setContent {
            CompositionLocalProvider(LocalHubStrings provides FrenchHubStrings) {
                TheHubTheme {
                    HubBottomNavigationBar(
                        selectedTab = MainTab.FEED,
                        onTabSelected = { selectedTab = it }
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag(MainTab.SEARCH.testTag).performClick()

        assertEquals(MainTab.SEARCH, selectedTab)
    }
}

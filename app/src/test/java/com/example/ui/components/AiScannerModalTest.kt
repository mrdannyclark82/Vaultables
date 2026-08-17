package com.example.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.ui.theme.VaultTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class AiScannerModalTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun scanner_modal_initial_state() {
        composeTestRule.setContent {
            VaultTheme {
                AiScannerModal(
                    isScanning = false,
                    scanMessage = "Align item in frame",
                    onDismiss = {},
                    onConfirmScan = { _, _, _, _, _, _, _, _ -> }
                )
            }
        }

        // Wait for idle to ensure rendering is complete
        composeTestRule.waitForIdle()

        // Capture image to specified path
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/scanner_modal_initial.png")
    }
}

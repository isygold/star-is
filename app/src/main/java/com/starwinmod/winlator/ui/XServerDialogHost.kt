package com.starwinmod.winlator.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import com.starwinmod.winlator.ui.dialogs.ActiveWindowsDialog
import com.starwinmod.winlator.ui.dialogs.DebugDialogContent
import com.starwinmod.winlator.ui.dialogs.InputControlsDialog
import com.starwinmod.winlator.ui.dialogs.ScreenEffectsDialog
import com.starwinmod.winlator.ui.dialogs.VibrationDialog
import com.starwinmod.winlator.ui.overlays.FSROverlay
import com.starwinmod.winlator.ui.overlays.MagnifierOverlay
import com.starwinmod.winlator.ui.theme.WinlatorTheme

fun setupDialogHost(view: ComposeView) {
    view.setContent {
        WinlatorTheme {
            XServerDialogHost()
        }
    }
}

@Composable
fun XServerDialogHost() {
    val state = XServerDialogState
    val activeDialog     by state.activeDialog.collectAsState()
    val magnifierVisible by state.magnifierVisible.collectAsState()
    val fsrVisible       by state.fsrVisible.collectAsState()

    when (activeDialog) {
        XServerDialogState.ActiveDialog.VIBRATION      -> VibrationDialog(state)
        XServerDialogState.ActiveDialog.DEBUG          -> DebugDialogContent(state)
        XServerDialogState.ActiveDialog.INPUT_CONTROLS -> InputControlsDialog(state)
        XServerDialogState.ActiveDialog.SCREEN_EFFECTS -> ScreenEffectsDialog(state)
        XServerDialogState.ActiveDialog.ACTIVE_WINDOWS -> ActiveWindowsDialog(state)
        XServerDialogState.ActiveDialog.NONE           -> Unit
    }

    if (magnifierVisible) MagnifierOverlay(state)
    if (fsrVisible) FSROverlay(state)
}

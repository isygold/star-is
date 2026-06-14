package com.starwinmod.winlator.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.starwinmod.winlator.lsfg.LsfgState

enum class TabType {
    GRAPHICS, HUD, CONTROLS, ADVANCED, TASK_MANAGER
}

object XServerDrawerState {

    private val _selectedTab = MutableStateFlow(TabType.GRAPHICS)
    val selectedTab: StateFlow<TabType> = _selectedTab

    fun selectTab(tab: TabType) { _selectedTab.value = tab }

    private val _isPaused                = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean>     = _isPaused

    private val _isRelativeMouseMovement = MutableStateFlow(false)
    val isRelativeMouseMovement: StateFlow<Boolean> = _isRelativeMouseMovement

    private val _isMouseDisabled         = MutableStateFlow(false)
    val isMouseDisabled: StateFlow<Boolean> = _isMouseDisabled

    private val _moveCursorToTouchpoint  = MutableStateFlow(false)
    val moveCursorToTouchpoint: StateFlow<Boolean> = _moveCursorToTouchpoint

    private val _showLogs                = MutableStateFlow(false)
    val showLogs: StateFlow<Boolean>     = _showLogs

    private val _showMagnifier           = MutableStateFlow(true)
    val showMagnifier: StateFlow<Boolean> = _showMagnifier

    private val _cursorExpanded          = MutableStateFlow(false)
    val cursorExpanded: StateFlow<Boolean> = _cursorExpanded

    private val _fpsExpanded = MutableStateFlow(false)
    val fpsExpanded: StateFlow<Boolean> = _fpsExpanded

    private val _fpsConfig = MutableStateFlow("")
    val fpsConfig: StateFlow<String> = _fpsConfig

    // Callbacks wired by XServerDisplayActivity.
    // @JvmField exposes these as public fields so Java can assign them directly.
    // Runnable avoids the kotlin.Unit return-type mismatch for Java void lambdas.
    @JvmField var onClose:                  Runnable? = null
    @JvmField var onKeyboard:               Runnable? = null
    @JvmField var onInputControls:          Runnable? = null
    @JvmField var onScreenEffects:          Runnable? = null
    @JvmField var onGraphicEngine:          Runnable? = null
    @JvmField var onVibration:              Runnable? = null
    @JvmField var onToggleFullscreen:       Runnable? = null
    @JvmField var onPauseResume:            Runnable? = null
    @JvmField var onPipMode:               Runnable? = null
    @JvmField var onActiveWindows:          Runnable? = null
    @JvmField var onTaskManager:            Runnable? = null
    @JvmField var onMagnifier:              Runnable? = null
    @JvmField var onLogs:                   Runnable? = null
    @JvmField var onExit:                   Runnable? = null
    @JvmField var onMoveCursorToTouchpoint: Runnable? = null
    @JvmField var onRelativeMouseMovement:  Runnable? = null
    @JvmField var onDisableMouse:           Runnable? = null
    @JvmField var onFpsConfigApply: XServerDialogState.FpsConfigCallback? = null
    var onCursorExpandedChanged: ((Boolean) -> Unit)? = null
    @JvmField var onLsfgToggle: Runnable? = null
    @JvmField var onApplyLsfg: Runnable? = null
    @JvmField var onResetLsfg: Runnable? = null

    // Setters called from Java
    fun setIsPaused(v: Boolean)                { _isPaused.value = v }
    fun setIsRelativeMouseMovement(v: Boolean) { _isRelativeMouseMovement.value = v }
    fun setIsMouseDisabled(v: Boolean)         { _isMouseDisabled.value = v }
    fun setMoveCursorToTouchpoint(v: Boolean)  { _moveCursorToTouchpoint.value = v }
    fun setShowLogs(v: Boolean)                { _showLogs.value = v }
    fun setShowMagnifier(v: Boolean)           { _showMagnifier.value = v }
    fun setCursorExpanded(v: Boolean)          { _cursorExpanded.value = v }

    fun toggleCursorExpanded() {
        val next = !_cursorExpanded.value
        _cursorExpanded.value = next
        onCursorExpandedChanged?.invoke(next)
    }

    fun setFpsExpanded(v: Boolean) { _fpsExpanded.value = v }
    fun setFpsConfig(v: String) { _fpsConfig.value = v }
    fun toggleFpsExpanded() { _fpsExpanded.value = !_fpsExpanded.value }


    // ── LSFG delegation (backed by LsfgState.Global) ─────────────────────────
    // These methods are kept for backward compatibility; new code should use
    // LsfgState.Global directly.

    @Deprecated("Use LsfgState.Global.setEnabled()", ReplaceWith("LsfgState.Global.setEnabled(v)"))
    fun setLsfgEnabled(v: Boolean) { LsfgState.Global.setEnabled(v) }

    @Deprecated("Use LsfgState.Global.snapshot().multiplier", ReplaceWith("LsfgState.Global.getLsfgMultiplier()"))
    fun getLsfgEnabled(): Boolean = LsfgState.Global.getLsfgEnabled()

    @Deprecated("Use LsfgState.Global.setMultiplier()", ReplaceWith("LsfgState.Global.setMultiplier(v)"))
    fun setLsfgMultiplier(v: Int) { LsfgState.Global.setMultiplier(v) }

    @Deprecated("Use LsfgState.Global.getLsfgMultiplier()", ReplaceWith("LsfgState.Global.getLsfgMultiplier()"))
    fun getLsfgMultiplier(): Int = LsfgState.Global.getLsfgMultiplier()

    @Deprecated("Use LsfgState.Global.setQuality()", ReplaceWith("LsfgState.Global.setQuality(v)"))
    fun setLsfgQuality(v: String) { LsfgState.Global.setQuality(v) }

    @Deprecated("Use LsfgState.Global.getLsfgQuality()", ReplaceWith("LsfgState.Global.getLsfgQuality()"))
    fun getLsfgQuality(): String = LsfgState.Global.getLsfgQuality()

    @Deprecated("Use LsfgState.Global.setFlowScale()", ReplaceWith("LsfgState.Global.setFlowScale(v)"))
    fun setLsfgFlowScale(v: Int) { LsfgState.Global.setFlowScale(v) }

    @Deprecated("Use LsfgState.Global.getLsfgFlowScale()", ReplaceWith("LsfgState.Global.getLsfgFlowScale()"))
    fun getLsfgFlowScale(): Int = LsfgState.Global.getLsfgFlowScale()

    @Deprecated("Use LsfgState.Global.setMaxLatency()", ReplaceWith("LsfgState.Global.setMaxLatency(v)"))
    fun setLsfgMaxLatency(v: Int) { LsfgState.Global.setMaxLatency(v) }

    @Deprecated("Use LsfgState.Global.getLsfgMaxLatency()", ReplaceWith("LsfgState.Global.getLsfgMaxLatency()"))
    fun getLsfgMaxLatency(): Int = LsfgState.Global.getLsfgMaxLatency()

    @Deprecated("Use LsfgState.Global.setGpuArch()", ReplaceWith("LsfgState.Global.setGpuArch(v)"))
    fun setLsfgGpuArch(v: String) { LsfgState.Global.setGpuArch(v) }

    @Deprecated("Use LsfgState.Global.getLsfgGpuArch()", ReplaceWith("LsfgState.Global.getLsfgGpuArch()"))
    fun getLsfgGpuArch(): String = LsfgState.Global.getLsfgGpuArch()


    fun reset() {
        _selectedTab.value = TabType.GRAPHICS
        _isPaused.value = false
        _isRelativeMouseMovement.value = false
        _isMouseDisabled.value = false
        _moveCursorToTouchpoint.value = false
        _showLogs.value = false
        _showMagnifier.value = true

        _cursorExpanded.value = false
        _fpsExpanded.value = false
        _fpsConfig.value = ""
        onClose = null; onKeyboard = null; onInputControls = null
        onScreenEffects = null; onGraphicEngine = null; onVibration = null
        onToggleFullscreen = null; onPauseResume = null; onPipMode = null
        onActiveWindows = null; onTaskManager = null; onMagnifier = null
        onLogs = null; onExit = null; onLsfgToggle = null; onMoveCursorToTouchpoint = null
        onRelativeMouseMovement = null; onDisableMouse = null
        onFpsConfigApply = null
        onCursorExpandedChanged = null
    }
}

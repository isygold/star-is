package com.starwinmod.winlator.lsfg

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Observable Compose-friendly state for the LSFG feature.
 *
 * Replaces the ad-hoc LSFG fields in [XServerDrawerState] with a
 * single, focused state object. Wire it in your Activity / Fragment:
 *
 *     val lsfgState = remember { LsfgState(initialConfig) }
 *
 * or use the singleton [LsfgState.Global] for the XServer overlay.
 */
class LsfgState(initialConfig: LsfgConfig = LsfgConfig.DEFAULT) {

    // ── Backing flows ────────────────────────────────────────────────────────

    private val _enabled     = MutableStateFlow(initialConfig.multiplier > 0)
    private val _config      = MutableStateFlow(initialConfig.validated())

    // ── Public readonly state ────────────────────────────────────────────────

    val enabled: StateFlow<Boolean>        = _enabled.asStateFlow()
    val config: StateFlow<LsfgConfig>      = _config.asStateFlow()

    // ── Derived convenience flows (avoid reconstructing LsfgConfig each time) ─

    val multiplier: StateFlow<Int>         = MutableStateFlow(initialConfig.multiplier).also {
        _config.subscribe { v -> it.value = v.multiplier }
    }
    val quality: StateFlow<String>         = MutableStateFlow(initialConfig.quality).also {
        _config.subscribe { v -> it.value = v.quality }
    }
    val flowScale: StateFlow<Int>          = MutableStateFlow(initialConfig.flowScale).also {
        _config.subscribe { v -> it.value = v.flowScale }
    }
    val maxLatency: StateFlow<Int>         = MutableStateFlow(initialConfig.maxLatency).also {
        _config.subscribe { v -> it.value = v.maxLatency }
    }
    val gpuArch: StateFlow<String>         = MutableStateFlow(initialConfig.gpuArch).also {
        _config.subscribe { v -> it.value = v.gpuArch }
    }
    val customDllEnabled: StateFlow<Boolean> = MutableStateFlow(initialConfig.customDllEnabled).also {
        _config.subscribe { v -> it.value = v.customDllEnabled }
    }
    val customDllPath: StateFlow<String>   = MutableStateFlow(initialConfig.customDllPath).also {
        _config.subscribe { v -> it.value = v.customDllPath }
    }

    // ── Mutation ─────────────────────────────────────────────────────────────

    fun setEnabled(v: Boolean) { _enabled.value = v }
    fun setConfig(cfg: LsfgConfig) {
        _config.value = cfg.validated()
    }

    fun setMultiplier(v: Int) {
        _config.value = _config.value.copy(multiplier = v).validated()
    }
    fun setQuality(v: String) {
        _config.value = _config.value.copy(quality = v).validated()
    }
    fun setFlowScale(v: Int) {
        _config.value = _config.value.copy(flowScale = v).validated()
    }
    fun setMaxLatency(v: Int) {
        _config.value = _config.value.copy(maxLatency = v).validated()
    }
    fun setGpuArch(v: String) {
        _config.value = _config.value.copy(gpuArch = v)
    }
    fun setCustomDllEnabled(v: Boolean) {
        _config.value = _config.value.copy(customDllEnabled = v)
    }
    fun setCustomDllPath(v: String) {
        _config.value = _config.value.copy(customDllPath = v)
    }

    /** Reset all values to defaults. */
    fun reset() {
        _enabled.value = false
        _config.value = LsfgConfig.DEFAULT
    }

    /** Convenience: update multiple properties at once from a [LsfgConfig]. */
    fun applyConfig(cfg: LsfgConfig) {
        _enabled.value = cfg.multiplier > 0
        _config.value = cfg.validated()
    }

    /** [LsfgConfig] snapshot of the current state. */
    fun snapshot(): LsfgConfig = _config.value

    // ── Global singleton for the XServerDrawer overlay ───────────────────────
    // (preserves the existing singleton pattern used by XServerDrawerState)

    object Global {
        private val state = LsfgState()

        val enabled: StateFlow<Boolean>        = state.enabled
        val config: StateFlow<LsfgConfig>      = state.config
        val multiplier: StateFlow<Int>         = state.multiplier
        val quality: StateFlow<String>         = state.quality
        val flowScale: StateFlow<Int>          = state.flowScale
        val maxLatency: StateFlow<Int>         = state.maxLatency
        val gpuArch: StateFlow<String>         = state.gpuArch
        val customDllEnabled: StateFlow<Boolean> = state.customDllEnabled
        val customDllPath: StateFlow<String>   = state.customDllPath

        fun setEnabled(v: Boolean) = state.setEnabled(v)
        fun setConfig(cfg: LsfgConfig) = state.setConfig(cfg)
        fun setMultiplier(v: Int) = state.setMultiplier(v)
        fun setQuality(v: String) = state.setQuality(v)
        fun setFlowScale(v: Int) = state.setFlowScale(v)
        fun setMaxLatency(v: Int) = state.setMaxLatency(v)
        fun setGpuArch(v: String) = state.setGpuArch(v)
        fun setCustomDllEnabled(v: Boolean) = state.setCustomDllEnabled(v)
        fun setCustomDllPath(v: String) = state.setCustomDllPath(v)
        fun reset() = state.reset()
        fun snapshot(): LsfgConfig = state.snapshot()
        fun applyConfig(cfg: LsfgConfig) = state.applyConfig(cfg)
        fun getLsfgEnabled(): Boolean = state._enabled.value
        fun getLsfgMultiplier(): Int = state._config.value.multiplier
        fun getLsfgQuality(): String = state._config.value.quality
        fun getLsfgFlowScale(): Int = state._config.value.flowScale
        fun getLsfgMaxLatency(): Int = state._config.value.maxLatency
        fun getLsfgGpuArch(): String = state._config.value.gpuArch
    }
}

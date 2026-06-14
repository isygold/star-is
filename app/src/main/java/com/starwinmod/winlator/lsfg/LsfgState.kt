package com.starwinmod.winlator.lsfg

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Observable Compose-friendly state for the LSFG (Vegas FrameGen) feature.
 *
 * Manages both the master enabled/disabled toggle and the full [LsfgConfig]
 * parameter set. New code should read [enabled] and [config] flows directly.
 *
 * Use [LsfgState.Global] as the singleton instance wired by XServerDisplayActivity,
 * or instantiate your own for previews / tests.
 */
class LsfgState(initialConfig: LsfgConfig = LsfgConfig.DEFAULT) {

    private val _enabled = MutableStateFlow(true)
    private val _config  = MutableStateFlow(initialConfig.validated())

    /** Whether frame generation is currently enabled. */
    val enabled: StateFlow<Boolean>   = _enabled.asStateFlow()

    /** The full parameter set (only meaningful when [enabled] is `true`). */
    val config: StateFlow<LsfgConfig> = _config.asStateFlow()

    // ── Mutators ─────────────────────────────────────────────────────────────

    fun setEnabled(v: Boolean) { _enabled.value = v }

    /** Replace the entire config atomically (clamped to valid ranges). */
    fun setConfig(cfg: LsfgConfig) { _config.value = cfg.validated() }

    fun setMultiplier(v: Int)     { _config.value = _config.value.copy(multiplier = v).validated() }
    fun setQuality(v: String)     { _config.value = _config.value.copy(quality = v).validated() }
    fun setFlowScale(v: Int)      { _config.value = _config.value.copy(flowScale = v).validated() }
    fun setMaxLatency(v: Int)     { _config.value = _config.value.copy(maxLatency = v).validated() }
    fun setGpuArch(v: String)     { _config.value = _config.value.copy(gpuArch = v) }
    fun setCustomDllEnabled(v: Boolean) { _config.value = _config.value.copy(customDllEnabled = v) }
    fun setCustomDllPath(v: String)     { _config.value = _config.value.copy(customDllPath = v) }

    /** Snapshot of the current config. */
    fun snapshot(): LsfgConfig = _config.value

    /** Reset everything to defaults. */
    fun reset() {
        _enabled.value = false
        _config.value = LsfgConfig.DEFAULT
    }

    // ── Global singleton ─────────────────────────────────────────────────────

    object Global {
        private val _inner = LsfgState()

        val enabled: StateFlow<Boolean>   = _inner.enabled
        val config: StateFlow<LsfgConfig> = _inner.config

        fun setEnabled(v: Boolean)              = _inner.setEnabled(v)
        fun setConfig(cfg: LsfgConfig)          = _inner.setConfig(cfg)
        fun setMultiplier(v: Int)               = _inner.setMultiplier(v)
        fun setQuality(v: String)               = _inner.setQuality(v)
        fun setFlowScale(v: Int)                = _inner.setFlowScale(v)
        fun setMaxLatency(v: Int)               = _inner.setMaxLatency(v)
        fun setGpuArch(v: String)               = _inner.setGpuArch(v)
        fun setCustomDllEnabled(v: Boolean)     = _inner.setCustomDllEnabled(v)
        fun setCustomDllPath(v: String)         = _inner.setCustomDllPath(v)
        fun snapshot(): LsfgConfig              = _inner.snapshot()
        fun reset()                             = _inner.reset()

        // Java-compatible getters (used by XServerDrawerState delegation)
        fun getLsfgEnabled(): Boolean    = _inner.enabled.value
        fun getLsfgMultiplier(): Int     = _inner.config.value.multiplier
        fun getLsfgQuality(): String     = _inner.config.value.quality
        fun getLsfgFlowScale(): Int      = _inner.config.value.flowScale
        fun getLsfgMaxLatency(): Int     = _inner.config.value.maxLatency
        fun getLsfgGpuArch(): String     = _inner.config.value.gpuArch
        fun isLsfgCustomDllEnabled(): Boolean = _inner.config.value.customDllEnabled
        fun getLsfgCustomDllPath(): String    = _inner.config.value.customDllPath
    }
}

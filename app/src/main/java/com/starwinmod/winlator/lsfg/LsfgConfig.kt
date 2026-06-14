package com.starwinmod.winlator.lsfg

import kotlin.jvm.JvmName

/**
 * Immutable configuration for the LSFG (Lossless Scaling Frame Generation) layer.
 *
 * All values are validated on construction via [validate]; use [tryCreate] or
 * [validated] to obtain a guaranteed-valid instance.
 *
 * @property multiplier  Frame multiplier (2–10, default 2)
 * @property quality     Quality preset: "performance", "balanced", or "quality"
 * @property flowScale   Motion-estimation flow scale in percent (50–200)
 * @property maxLatency  Maximum input latency in milliseconds (0–33)
 * @property gpuArch     GPU architecture hint: "auto", "adreno", "mali", etc.
 * @property customDllEnabled  Whether to inject a user-provided lossless.dll
 * @property customDllPath     Filesystem path to the lossless.dll on the host
 */
data class LsfgConfig(
    val multiplier: Int = 2,
    val quality: String = "balanced",
    val flowScale: Int = 100,
    val maxLatency: Int = 16,
    val gpuArch: String = "auto",
    @get:JvmName("isCustomDllEnabled")
    val customDllEnabled: Boolean = false,
    val customDllPath: String = ""
) {
    companion object {
        const val MIN_MULTIPLIER = 2
        const val MAX_MULTIPLIER = 10
        const val MIN_FLOW_SCALE = 50
        const val MAX_FLOW_SCALE = 200
        const val MIN_MAX_LATENCY = 0
        const val MAX_MAX_LATENCY = 33

        val VALID_QUALITIES = setOf("performance", "balanced", "quality")

        /** Default instance with all factory values. */
        val DEFAULT = LsfgConfig()

        /** Attempt to create a validated config, returning null if invalid. */
        fun tryCreate(
            multiplier: Int = 2,
            quality: String = "balanced",
            flowScale: Int = 100,
            maxLatency: Int = 16,
            gpuArch: String = "auto",
            customDllEnabled: Boolean = false,
            customDllPath: String = ""
        ): LsfgConfig? {
            val cfg = LsfgConfig(multiplier, quality, flowScale, maxLatency, gpuArch, customDllEnabled, customDllPath)
            return if (cfg.validate() == null) cfg else null
        }
    }

    /** Returns `null` if valid, or an error message string if invalid. */
    fun validate(): String? {
        if (multiplier < MIN_MULTIPLIER || multiplier > MAX_MULTIPLIER)
            return "multiplier must be between $MIN_MULTIPLIER and $MAX_MULTIPLIER, got $multiplier"
        if (quality !in VALID_QUALITIES)
            return "quality must be one of $VALID_QUALITIES, got '$quality'"
        if (flowScale < MIN_FLOW_SCALE || flowScale > MAX_FLOW_SCALE)
            return "flowScale must be between $MIN_FLOW_SCALE and $MAX_FLOW_SCALE, got $flowScale"
        if (maxLatency < MIN_MAX_LATENCY || maxLatency > MAX_MAX_LATENCY)
            return "maxLatency must be between $MIN_MAX_LATENCY and $MAX_MAX_LATENCY, got $maxLatency"
        return null
    }

    /** Returns a copy with all values clamped to valid ranges. Never throws. */
    fun validated(): LsfgConfig = copy(
        multiplier = multiplier.coerceIn(MIN_MULTIPLIER, MAX_MULTIPLIER),
        quality = if (quality in VALID_QUALITIES) quality else DEFAULT.quality,
        flowScale = flowScale.coerceIn(MIN_FLOW_SCALE, MAX_FLOW_SCALE),
        maxLatency = maxLatency.coerceIn(MIN_MAX_LATENCY, MAX_MAX_LATENCY),
        customDllPath = customDllPath ?: ""
    )
}

package com.starwinmod.winlator.lsfg

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.starwinmod.winlator.ui.theme.GlowPurple
import com.starwinmod.winlator.ui.theme.Primary
import com.starwinmod.winlator.ui.theme.PrimaryDim

/**
 * Self-contained settings panel for the LSFG (Vegas FrameGen) feature.
 *
 * Renders a toggle to enable/disable frame generation, plus configurable
 * parameters: multiplier, quality, flow scale, max input latency, and
 * custom lossless.dll support.
 *
 * Usage:
 * ```kotlin
 * LsfgSettingsPanel(
 *     config = lsfgConfig,
 *     onToggle = { enabled -> ... },
 *     onChange = { newConfig -> ... },
 *     onReset = { ... }
 * )
 * ```
 *
 * @param config        Current [LsfgConfig].
 * @param onToggle      Called when the master toggle is switched.
 * @param onChange      Called with a new [LsfgConfig] when any parameter changes.
 * @param onReset       Called when "Reset to Defaults" is pressed.
 * @param modifier      Optional [Modifier].
 */
@Composable
fun LsfgSettingsPanel(
    config: LsfgConfig,
    onToggle: (Boolean) -> Unit,
    onChange: (LsfgConfig) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // ── Master toggle ────────────────────────────────────────────────
        SectionHeader("Vegas FrameGen")

        ToggleRow(
            label = "Enable Frame Generation",
            subtitle = "Multi-frame interpolation via LSFG Vulkan layer",
            checked = config.multiplier > 0,
            onCheckedChange = onToggle
        )

        // ── Parameters (only shown when enabled) ─────────────────────────
        if (config.multiplier > 0) {
            Spacer(Modifier.height(8.dp))

            Text(
                text = "Interpolation",
                color = Primary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
            Spacer(Modifier.height(4.dp))

            // Multiplier
            LsfgDropdown(
                label = "Multiplier",
                options = listOf("2x", "3x", "4x", "5x", "6x", "7x", "8x", "9x", "10x"),
                selectedOption = "${config.multiplier}x"
            ) { opt ->
                val num = opt.removeSuffix("x").toIntOrNull() ?: 2
                onChange(config.copy(multiplier = num))
            }

            // Quality
            LsfgDropdown(
                label = "Quality",
                options = listOf("performance", "balanced", "quality"),
                selectedOption = config.quality
            ) { opt ->
                onChange(config.copy(quality = opt))
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Advanced",
                color = Primary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
            Spacer(Modifier.height(4.dp))

            // Flow Scale
            LsfgSlider(
                label = "Flow Scale",
                value = config.flowScale.toFloat(),
                valueRange = 50f..200f,
                onValueChange = { onChange(config.copy(flowScale = it.toInt())) },
                format = { "${it.toInt()}%" }
            )

            // Max Input Latency
            LsfgSlider(
                label = "Max Input Latency",
                value = config.maxLatency.toFloat(),
                valueRange = 0f..33f,
                onValueChange = { onChange(config.copy(maxLatency = it.toInt())) },
                format = { "${it.toInt()}ms" }
            )

            Spacer(Modifier.height(6.dp))

            // ── Custom DLL section ───────────────────────────────────────
            HorizontalDivider(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Text(
                text = "Custom DLL",
                color = Primary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
            Spacer(Modifier.height(4.dp))

            ToggleRow(
                label = "Use custom lossless.dll",
                subtitle = "Override the built-in frame-gen implementation",
                checked = config.customDllEnabled,
                onCheckedChange = { onChange(config.copy(customDllEnabled = it)) }
            )

            if (config.customDllEnabled) {
                Text(
                    text = "Place lossless.dll at: " + config.customDllPath.ifEmpty { "<not set>" },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                )
                Spacer(Modifier.height(4.dp))
            }

            Spacer(Modifier.height(8.dp))

            // ── Reset button ─────────────────────────────────────────────
            Button(
                onClick = onReset,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryDim,
                    contentColor = Color.White
                )
            ) {
                Text("Reset to Defaults", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Reusable private composables
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = Primary,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
    )
}

@Composable
private fun ToggleRow(
    label: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = GlowPurple,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.40f),
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LsfgDropdown(
    label: String,
    options: List<String>,
    selectedOption: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            singleLine = true
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = { onSelect(opt); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun LsfgSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    format: (Float) -> String = { "%.0f".format(it) }
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = format(value),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = GlowPurple,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

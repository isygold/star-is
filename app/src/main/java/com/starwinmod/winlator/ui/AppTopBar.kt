package com.starwinmod.winlator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.starwinmod.winlator.ui.theme.Primary
import com.starwinmod.winlator.ui.theme.Secondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    showBack: Boolean = false,
    onNavClick: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TopAppBar(
            title = { Text(text = title, color = MaterialTheme.colorScheme.onSurface) },
            navigationIcon = {
                IconButton(onClick = onNavClick) {
                    if (showBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Open menu",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            },
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
            ),
        )
        // VEGAS gradient bottom border
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Primary, Secondary)
                    )
                )
        )
    }
}

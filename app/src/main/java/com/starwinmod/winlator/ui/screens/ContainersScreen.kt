package com.starwinmod.winlator.ui.screens

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import android.widget.Toast
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import java.io.File
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.starwinmod.winlator.ui.LocalTopBarActions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.starwinmod.winlator.XServerDisplayActivity
import com.starwinmod.winlator.XrActivity
import com.starwinmod.winlator.container.Container
import com.starwinmod.winlator.core.FileUtils
import com.starwinmod.winlator.core.StringUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.starwinmod.winlator.ui.theme.Divider as DividerColor
import com.starwinmod.winlator.ui.theme.GlowPurple
import com.starwinmod.winlator.ui.theme.OnSurface
import com.starwinmod.winlator.ui.theme.OnSurfaceVariant
import com.starwinmod.winlator.ui.theme.Primary
import com.starwinmod.winlator.ui.theme.PrimaryDim
import com.starwinmod.winlator.ui.theme.Secondary
import com.starwinmod.winlator.ui.theme.Surface as SurfaceColor
import com.starwinmod.winlator.ui.theme.Tertiary
import com.starwinmod.winlator.xenvironment.ImageFs

@Composable
fun ContainersScreen(
    onNavigateToDetail: (containerId: Int?) -> Unit,
    vm: ContainersViewModel = viewModel(),
) {
    val containers by vm.containers.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val context = LocalContext.current
    val activity = context as Activity

    // Refresh list whenever this screen resumes (e.g. returning from ContainerDetail)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Confirm-dialog state
    var confirmDialog by remember { mutableStateOf<ConfirmAction?>(null) }
    var storageInfoContainer by remember { mutableStateOf<Container?>(null) }
    var showImportPicker by remember { mutableStateOf(false) }

    val topBarActions = LocalTopBarActions.current
    // LaunchedEffect — not SideEffect — so this runs in the same dispatcher queue as
    // MainActivity's route-change clear (parent enqueues first, we enqueue second and
    // run after). A SideEffect would set during commit and the parent's post-commit
    // clear would steamroll it on first navigation to this screen.
    LaunchedEffect(Unit) {
        topBarActions.value = {
            IconButton(onClick = { showImportPicker = true }) {
                Icon(Icons.Filled.FileDownload, contentDescription = "Import container", tint = androidx.compose.ui.graphics.Color.White)
            }
        }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        // ── Search bar ──
        var searchQuery by remember { mutableStateOf("") }
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search containers\u2026", color = OnSurfaceVariant) },
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = null, tint = OnSurfaceVariant)
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = OnSurface,
                unfocusedTextColor = OnSurface,
                cursorColor = Primary,
                focusedBorderColor = Primary.copy(alpha = 0.6f),
                unfocusedBorderColor = DividerColor,
                focusedContainerColor = SurfaceColor,
                unfocusedContainerColor = SurfaceColor,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )

        // ── Section header ──
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            Text(
                text = "CONTAINERS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = GlowPurple,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "${containers.size}",
                fontSize = 11.sp,
                color = OnSurfaceVariant,
            )
        }
        // Gradient underline
        Box(
            modifier = Modifier
                .fillMaxWidth(0.25f)
                .height(2.dp)
                .padding(horizontal = 16.dp)
                .background(
                    Brush.horizontalGradient(listOf(GlowPurple, GlowPurple.copy(alpha = 0.1f))),
                    RoundedCornerShape(1.dp)
                )
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val filtered = if (searchQuery.isBlank()) containers
                           else containers.filter { it.name.contains(searchQuery, ignoreCase = true) }

            if (filtered.isEmpty() && !isLoading) {
                // ── VEGAS empty state ──
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                ) {
                    // Decorative circles
                    Box(Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                        Box(
                            Modifier
                                .size(80.dp)
                                .background(PrimaryDim.copy(alpha = 0.2f), RoundedCornerShape(40.dp))
                        )
                        Box(
                            Modifier
                                .size(56.dp)
                                .background(Primary.copy(alpha = 0.15f), RoundedCornerShape(28.dp))
                        )
                        Icon(
                            Icons.Filled.FolderOpen,
                            contentDescription = null,
                            tint = Primary.copy(alpha = 0.5f),
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "No containers match \"$searchQuery\""
                               else "Your library is empty",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = OnSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "Try a different search term"
                               else "Tap + to create your first container",
                        fontSize = 13.sp,
                        color = OnSurfaceVariant,
                    )
                }
            } else if (!isLoading) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(filtered, key = { it.id }) { container ->
                        VegasContainerCard(
                            container = container,
                            onRun = {
                                if (!XrActivity.isEnabled(context)) {
                                    val intent = Intent(context, XServerDisplayActivity::class.java)
                                    intent.putExtra("container_id", container.id)
                                    context.startActivity(intent)
                                } else {
                                    XrActivity.openIntent(activity, container.id, null)
                                }
                            },
                            onEdit = { onNavigateToDetail(container.id) },
                            onDuplicate = {
                                confirmDialog = ConfirmAction.Duplicate(container)
                            },
                            onRemove = {
                                confirmDialog = ConfirmAction.Remove(container)
                            },
                            onExport = {
                                vm.exportContainer(container) { path ->
                                    val msg = if (path != null)
                                        "Exported to $path"
                                    else
                                        "Export failed or already exists"
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            },
                            onInfo = { storageInfoContainer = container },
                        )
                    }
                }
            }

            // FAB
            FloatingActionButton(
                onClick = {
                    if (ImageFs.find(context).isValid()) onNavigateToDetail(null)
                },
                containerColor = Primary,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp),
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Add container")
            }

            // Loading overlay
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            }
        } // end inner Box
    } // end Column

    // Import picker dialog
    if (showImportPicker) {
        val backups = remember { vm.availableBackups() }
        AlertDialog(
            onDismissRequest = { showImportPicker = false },
            title = { Text("Import Container") },
            text = {
                if (backups.isEmpty()) {
                    Text("No exported containers found in Downloads/Winlator/Backups/Containers/.")
                } else {
                    androidx.compose.foundation.layout.Column {
                        backups.forEach { dir ->
                            TextButton(
                                onClick = {
                                    showImportPicker = false
                                    vm.importContainer(dir) {
                                        Toast.makeText(context, "Container imported: ${dir.name}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(dir.name, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showImportPicker = false }) { Text("Cancel") }
            },
        )
    }

    // Confirm dialogs
    confirmDialog?.let { action ->
        when (action) {
            is ConfirmAction.Duplicate -> {
                AlertDialog(
                    onDismissRequest = { confirmDialog = null },
                    title = { Text("Duplicate container?") },
                    text = { Text("Duplicate \"${action.container.name}\"?") },
                    confirmButton = {
                        TextButton(onClick = {
                            confirmDialog = null
                            vm.duplicate(action.container) {}
                        }) { Text("Duplicate") }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmDialog = null }) { Text("Cancel") }
                    },
                )
            }
            is ConfirmAction.Remove -> {
                AlertDialog(
                    onDismissRequest = { confirmDialog = null },
                    title = { Text("Remove container?") },
                    text = { Text("Remove \"${action.container.name}\" permanently?") },
                    confirmButton = {
                        TextButton(onClick = {
                            confirmDialog = null
                            vm.remove(action.container, context) {}
                        }) { Text("Remove") }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmDialog = null }) { Text("Cancel") }
                    },
                )
            }
        }
    }

    // Storage info dialog
    storageInfoContainer?.let { container ->
        StorageInfoDialog(container = container, onDismiss = { storageInfoContainer = null })
    }
}

@Composable
private fun VegasContainerCard(
    container: Container,
    onRun: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onRemove: () -> Unit,
    onExport: () -> Unit,
    onInfo: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    // Derive a consistent accent from container name
    val cardAccent = remember(container.id) {
        val colors = listOf(Primary, Secondary, GlowPurple, Tertiary)
        colors[container.id % colors.size]
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 3.dp,
        color = SurfaceColor,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // ── Left accent bar ──
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(76.dp)
                    .background(
                        Brush.verticalGradient(listOf(cardAccent, cardAccent.copy(alpha = 0.2f))),
                        shape = RoundedCornerShape(topEnd = 0.dp, bottomEnd = 0.dp),
                    )
            )

            // ── Main content ──
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            ) {
                // Container name
                Text(
                    text = container.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurface,
                    maxLines = 1,
                )

                Spacer(Modifier.height(2.dp))

                // Subtitle: path or info
                Text(
                    text = container.getRootDir()?.name ?: "Local",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant,
                    maxLines = 1,
                )

                Spacer(Modifier.height(8.dp))

                // Badge row
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // DX wrapper badge
                    val dxWrapper = container.dxWrapper
                    if (dxWrapper.isNotBlank()) {
                        val badgeColor = if (dxWrapper.contains("vegas", ignoreCase = true)) Primary
                                         else OnSurfaceVariant
                        Box(
                            modifier = Modifier
                                .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = if (dxWrapper.length > 12) dxWrapper.take(10) + "\u2026" else dxWrapper,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = badgeColor,
                            )
                        }
                    }

                    // Box64 indicator
                    if (container.box64Preset != 0) {
                        Box(
                            modifier = Modifier
                                .background(Tertiary.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = "\u26A0 Box64",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = Tertiary,
                            )
                        }
                    }
                }
            }

            // ── Actions column ──
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(end = 4.dp),
            ) {
                // Run button
                IconButton(onClick = onRun) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Run",
                        tint = Primary,
                        modifier = Modifier.size(28.dp),
                    )
                }
                // 3-dot menu
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Options",
                            tint = OnSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            leadingIcon = { Icon(Icons.Filled.Edit, null, tint = OnSurface) },
                            onClick = { menuExpanded = false; onEdit() },
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicate") },
                            leadingIcon = { Icon(Icons.Filled.ContentCopy, null, tint = OnSurface) },
                            onClick = { menuExpanded = false; onDuplicate() },
                        )
                        DropdownMenuItem(
                            text = { Text("Remove") },
                            leadingIcon = { Icon(Icons.Filled.Delete, null, tint = OnSurface) },
                            onClick = { menuExpanded = false; onRemove() },
                        )
                        DropdownMenuItem(
                            text = { Text("Export") },
                            leadingIcon = { Icon(Icons.Filled.FileUpload, null, tint = OnSurface) },
                            onClick = { menuExpanded = false; onExport() },
                        )
                        DropdownMenuItem(
                            text = { Text("Info") },
                            leadingIcon = { Icon(Icons.Filled.Info, null, tint = OnSurface) },
                            onClick = { menuExpanded = false; onInfo() },
                        )
                    }
                }
            }
        }
    }
}

private sealed class ConfirmAction {
    data class Duplicate(val container: Container) : ConfirmAction()
    data class Remove(val container: Container) : ConfirmAction()
}

@Composable
private fun StorageInfoDialog(container: Container, onDismiss: () -> Unit) {
    var driveCSize by remember { mutableLongStateOf(0L) }
    var cacheSize  by remember { mutableLongStateOf(0L) }
    var totalSize  by remember { mutableLongStateOf(0L) }
    val internalStorageSize = remember { FileUtils.getInternalStorageSize() }
    val progress = if (internalStorageSize > 0)
        ((totalSize.toFloat() / internalStorageSize) * 100f).coerceIn(0f, 100f)
    else 0f

    val handler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }

    LaunchedEffect(container) {
        val rootDir   = container.getRootDir()
        val driveCDir = File(rootDir, ".wine/drive_c")
        val cacheDir  = File(rootDir, ".cache")
        launch(Dispatchers.IO) {
            FileUtils.getSizeAsync(driveCDir) { size ->
                handler.post { driveCSize += size; totalSize += size }
            }
        }
        launch(Dispatchers.IO) {
            FileUtils.getSizeAsync(cacheDir) { size ->
                handler.post { cacheSize += size; totalSize += size }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Storage Info", fontWeight = FontWeight.Bold, color = OnSurface)
        },
        text = {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Left column — Drive C / Cache / Total sizes
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text("Drive C", fontSize = 12.sp, color = OnSurfaceVariant)
                    Text(StringUtils.formatBytes(driveCSize), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Primary)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Cache", fontSize = 12.sp, color = OnSurfaceVariant)
                    Text(StringUtils.formatBytes(cacheSize), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Primary)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Total", fontSize = 12.sp, color = OnSurfaceVariant)
                    Text(StringUtils.formatBytes(totalSize), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Primary)
                }
                // Right column — circular progress + label
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { progress / 100f },
                            modifier = Modifier.size(100.dp),
                            strokeWidth = 10.dp,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            color = Primary,
                        )
                        Text("${progress.toInt()}%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnSurface)
                    }
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        "Estimated used space",
                        fontSize = 11.sp,
                        color = OnSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = {
                FileUtils.clear(File(container.getRootDir(), ".cache"))
                container.putExtra("desktopTheme", null)
                container.saveData()
                onDismiss()
            }) { Text("Clear Cache", color = Primary) }
        },
    )
}

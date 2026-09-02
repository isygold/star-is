package com.winlator.star.ui.screens

import android.app.Activity
import android.content.Context
import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.winlator.star.contents.ContentProfile
import com.winlator.star.contents.ContentsManager
import com.winlator.star.contents.Downloader
import com.winlator.star.ui.theme.Divider as DividerColor
import com.winlator.star.ui.theme.GlowPurple
import com.winlator.star.ui.theme.OnSurface
import com.winlator.star.ui.theme.OnSurfaceVariant
import com.winlator.star.ui.theme.Primary
import com.winlator.star.ui.theme.Secondary
import com.winlator.star.ui.theme.Surface as SurfaceColor
import com.winlator.star.ui.theme.Tertiary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors

/**
 * Dialog showing downloadable + installed content items for given content type(s).
 * Works on any screen (including inside other dialogs).
 * After install/remove, calls [onContentChanged] so the parent can refresh version lists.
 */
@Composable
fun ContentDownloadSheet(
    contentTypes: List<ContentProfile.ContentType>,
    onDismiss: () -> Unit,
    onContentChanged: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as Activity
    val cm = remember { ContentsManager(context) }
    val scope = rememberCoroutineScope()

    var profiles by remember { mutableStateOf<List<ContentProfile>>(emptyList()) }
    var downloadingKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showInfoProfile by remember { mutableStateOf<ContentProfile?>(null) }
    var confirmRemoveProfile by remember { mutableStateOf<ContentProfile?>(null) }
    var installing by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isLoadingRemote by remember { mutableStateOf(true) }

    LaunchedEffect(contentTypes) {
        val json = withContext(Dispatchers.IO) {
            Downloader.downloadString(ContentsManager.REMOTE_PROFILES)
        }
        if (json != null) {
            cm.setRemoteProfiles(json)
        } else {
            cm.syncContents()
        }
        loadProfiles(cm, contentTypes) { profiles = it }
        isLoadingRemote = false
    }

    // Info sub-dialog
    showInfoProfile?.let { profile ->
        AlertDialog(
            onDismissRequest = { showInfoProfile = null },
            containerColor = SurfaceColor,
            title = { Text("Content Info", fontWeight = FontWeight.Bold, color = OnSurface) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    InfoField("Type", profile.type.toString())
                    InfoField("Version", profile.verName)
                    InfoField("Code", profile.verCode.toString())
                    if (!profile.desc.isNullOrEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(profile.desc, color = OnSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showInfoProfile = null }) { Text("OK", color = Primary) } }
        )
    }

    // Remove confirmation
    confirmRemoveProfile?.let { profile ->
        AlertDialog(
            onDismissRequest = { confirmRemoveProfile = null },
            containerColor = SurfaceColor,
            title = { Text("Remove content?", fontWeight = FontWeight.Bold, color = OnSurface) },
            text = { Text("Remove \"${profile.verName}\"?", color = OnSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = {
                    cm.removeContent(profile)
                    cm.syncContents()
                    loadProfiles(cm, contentTypes) { profiles = it }
                    confirmRemoveProfile = null
                    onContentChanged()
                }) { Text("Remove", color = Color(0xFFEF5350)) }
            },
            dismissButton = { TextButton(onClick = { confirmRemoveProfile = null }) { Text("Cancel", color = OnSurfaceVariant) } }
        )
    }

    // Installing overlay
    if (installing) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
            Surface(
                color = SurfaceColor,
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 12.dp,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    CircularProgressIndicator(color = Tertiary, strokeWidth = 3.dp, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Installing\u2026", fontWeight = FontWeight.SemiBold, color = OnSurface)
                }
            }
        }
    }

    // Error toast sub-dialog
    errorMsg?.let { msg ->
        AlertDialog(
            onDismissRequest = { errorMsg = null },
            containerColor = SurfaceColor,
            title = { Text("Error", fontWeight = FontWeight.Bold, color = OnSurface) },
            text = { Text(msg, color = OnSurfaceVariant) },
            confirmButton = { TextButton(onClick = { errorMsg = null }) { Text("OK", color = Primary) } }
        )
    }

    // Main dialog
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceColor,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column {
                Text(
                    "CONTENT DOWNLOADS",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Primary,
                    letterSpacing = 2.sp,
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    contentTypes.forEach { type ->
                        Icon(
                            imageVector = typeIcon(type),
                            contentDescription = null,
                            tint = GlowPurple,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            type.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant,
                        )
                        if (type != contentTypes.last()) {
                            Text(" + ", color = OnSurfaceVariant)
                        }
                    }
                }
            }
        },
        text = {
            if (isLoadingRemote) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            } else if (profiles.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No content available.", color = OnSurfaceVariant)
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    items(profiles, key = { ContentsManager.getEntryName(it) }) { profile ->
                        val key = ContentsManager.getEntryName(profile)
                        val isLocal = profile.remoteUrl == null
                        val isDownloading = key in downloadingKeys

                        DownloadContentItem(
                            profile = profile,
                            isLocal = isLocal,
                            isDownloading = isDownloading,
                            onDownload = {
                                downloadingKeys = downloadingKeys + key
                                scope.launch {
                                    val uri = withContext(Dispatchers.IO) {
                                        downloadToCache(context, profile)
                                    }
                                    downloadingKeys = downloadingKeys - key
                                    if (uri != null) {
                                        installing = true
                                        installContent(context, cm, uri) { ok ->
                                            installing = false
                                            if (ok) {
                                                loadProfiles(cm, contentTypes) { profiles = it }
                                                onContentChanged()
                                            } else {
                                                errorMsg = "Install failed."
                                            }
                                        }
                                    } else {
                                        errorMsg = "Download failed."
                                    }
                                }
                            },
                            onInfo = { showInfoProfile = profile },
                            onRemove = { confirmRemoveProfile = profile },
                        )
                        HorizontalDivider(color = DividerColor)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = OnSurfaceVariant) } },
    )
}

// ── Content download sheet for a single type ──────────────────────────────────
@Composable
fun ContentDownloadSheet(
    contentType: ContentProfile.ContentType,
    onDismiss: () -> Unit,
    onContentChanged: () -> Unit,
) = ContentDownloadSheet(listOf(contentType), onDismiss, onContentChanged)

// ── Internal composables ──────────────────────────────────────────────────────

@Composable
private fun DownloadContentItem(
    profile: ContentProfile,
    isLocal: Boolean,
    isDownloading: Boolean,
    onDownload: () -> Unit,
    onInfo: () -> Unit,
    onRemove: () -> Unit,
) {
    val type = profile.type
    val accentColor = when {
        type == ContentProfile.ContentType.CONTENT_TYPE_VEGAS -> Primary
        type == ContentProfile.ContentType.CONTENT_TYPE_WINE -> Secondary
        type == ContentProfile.ContentType.CONTENT_TYPE_DXVK -> GlowPurple
        else -> OnSurfaceVariant
    }

    Surface(
        color = SurfaceColor,
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Left gradient accent bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(52.dp)
                    .background(accentColor)
            )
            Spacer(Modifier.width(10.dp))
            // Content type icon
            Icon(
                imageVector = typeIcon(type),
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    profile.verName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = OnSurface,
                )
                Text(
                    if (isLocal) "Installed" else "Code ${profile.verCode}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isLocal) Tertiary else OnSurfaceVariant,
                )
            }
            if (!isLocal) {
                if (isDownloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp).padding(end = 4.dp),
                        strokeWidth = 2.dp,
                        color = Tertiary,
                    )
                } else {
                    IconButton(onClick = onDownload) {
                        Icon(Icons.Filled.Download, contentDescription = "Download", tint = Primary)
                    }
                }
            } else {
                IconButton(onClick = onInfo) {
                    Icon(Icons.Filled.Info, contentDescription = "Info", tint = OnSurfaceVariant)
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = Color(0xFFEF5350))
                }
            }
        }
    }
}

@Composable
private fun InfoField(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 3.dp)) {
        Text("$label: ", color = OnSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Text(value, color = OnSurface, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

// ── Per-type icon ──────────────────────────────────────────────────────────────

internal fun typeIcon(type: ContentProfile.ContentType): ImageVector = when (type) {
    ContentProfile.ContentType.CONTENT_TYPE_WINE     -> Icons.Filled.WineBar
    ContentProfile.ContentType.CONTENT_TYPE_DXVK     -> Icons.Filled.Memory
    ContentProfile.ContentType.CONTENT_TYPE_VKD3D    -> Icons.Filled.Memory
    ContentProfile.ContentType.CONTENT_TYPE_VEGAS    -> Icons.Filled.Settings
    ContentProfile.ContentType.CONTENT_TYPE_BOX64    -> Icons.Filled.Computer
    ContentProfile.ContentType.CONTENT_TYPE_WOWBOX64 -> Icons.Filled.Computer
    ContentProfile.ContentType.CONTENT_TYPE_FEXCORE  -> Icons.Filled.Memory
    else                                              -> Icons.Filled.Settings
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun loadProfiles(
    cm: ContentsManager,
    contentTypes: List<ContentProfile.ContentType>,
    onResult: (List<ContentProfile>) -> Unit,
) {
    cm.syncContents()
    val all = mutableListOf<ContentProfile>()
    for (type in contentTypes) {
        cm.getProfiles(type)?.let { all.addAll(it) }
    }
    onResult(all.distinctBy { ContentsManager.getEntryName(it) })
}

private fun downloadToCache(context: Context, profile: ContentProfile): Uri? {
    val f = File(context.cacheDir, "temp_${System.currentTimeMillis()}")
    return if (Downloader.downloadFile(profile.remoteUrl, f)) Uri.fromFile(f) else null
}

private fun installContent(
    context: Context,
    cm: ContentsManager,
    uri: Uri,
    onDone: (Boolean) -> Unit,
) {
    val activity = context as Activity
    Executors.newSingleThreadExecutor().execute {
        cm.extraContentFile(uri, object : ContentsManager.OnInstallFinishedCallback {
            var phase = 0
            override fun onFailed(reason: ContentsManager.InstallFailedReason, e: Exception?) {
                activity.runOnUiThread { onDone(false) }
            }
            override fun onSucceed(profile: ContentProfile) {
                if (phase == 0) {
                    phase = 1
                    cm.finishInstallContent(profile, this)
                } else {
                    cm.syncContents()
                    activity.runOnUiThread { onDone(true) }
                }
            }
        })
    }
}

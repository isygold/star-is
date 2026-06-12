package com.starwinmod.winlator.ui.screens

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.starwinmod.winlator.contents.ContentProfile
import com.starwinmod.winlator.contents.ContentsManager
import com.starwinmod.winlator.contents.Downloader
import com.starwinmod.winlator.ui.theme.Divider as DividerColor
import com.starwinmod.winlator.ui.theme.GlowPurple
import com.starwinmod.winlator.ui.theme.OnSurface
import com.starwinmod.winlator.ui.theme.OnSurfaceVariant
import com.starwinmod.winlator.ui.theme.Primary
import com.starwinmod.winlator.ui.theme.PrimaryDim
import com.starwinmod.winlator.ui.theme.Secondary
import com.starwinmod.winlator.ui.theme.Surface as SurfaceColor
import com.starwinmod.winlator.ui.theme.Tertiary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.util.concurrent.Executors

private data class VegasRelease(
    val tagName: String,
    val displayName: String,
    val wcpAssetUrl: String?,
    val rawZipAssetUrl: String?,
)

/** Tags to mark as "Recommended" or "Latest" based on semver pattern. */
private val RECOMMENDED_TAG_PREFIXES = listOf("2.7", "2.8")

@Composable
fun VegasDownloadSheet(
    onDismiss: () -> Unit,
    onContentChanged: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as Activity
    val cm = remember { ContentsManager(context) }
    val scope = rememberCoroutineScope()

    var releases by remember { mutableStateOf<List<VegasRelease>>(emptyList()) }
    var installedVersions by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var downloadingTag by remember { mutableStateOf<String?>(null) }
    var installing by remember { mutableStateOf(false) }

    // Load locally installed VEGAS profiles to mark them
    LaunchedEffect(Unit) {
        cm.syncContents()
        val installed = cm.profiles.filter {
            it.type == ContentProfile.CONTENT_TYPE_VEGAS
        }.mapNotNull { profile ->
            profile.verName?.removePrefix("vegas-")
        }.toSet()
        installedVersions = installed
    }

    // Fetch releases from GitHub API
    LaunchedEffect(Unit) {
        val json = withContext(Dispatchers.IO) {
            Downloader.downloadString("https://api.github.com/repos/isygold/vegas-releases/releases")
        }
        if (json != null) {
            try {
                val arr = JSONArray(json)
                val list = mutableListOf<VegasRelease>()
                for (i in 0 until arr.length()) {
                    val rel = arr.getJSONObject(i)
                    val tag = rel.getString("tag_name")
                    val name = rel.optString("name", tag)
                    var wcpUrl: String? = null
                    var zipUrl: String? = null
                    val assets = rel.optJSONArray("assets")
                    if (assets != null) {
                        for (j in 0 until assets.length()) {
                            val a = assets.getJSONObject(j)
                            val aname = a.getString("name")
                            if (aname.startsWith("vegas-") && aname.endsWith(".wcp")) {
                                wcpUrl = a.getString("browser_download_url")
                            } else if (aname.startsWith("dxvk-") && aname.endsWith(".zip")) {
                                zipUrl = a.getString("browser_download_url")
                            }
                        }
                    }
                    if (wcpUrl != null) {
                        list.add(VegasRelease(tag, name, wcpUrl, zipUrl))
                    }
                }
                releases = list
            } catch (e: Exception) {
                errorMsg = "Failed to parse releases: ${e.message}"
            }
        } else {
            errorMsg = "Failed to fetch releases from GitHub"
        }
        isLoading = false
    }

    // ── Installing overlay ──
    if (installing) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 12.dp,
                shadowElevation = 8.dp,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(36.dp),
                ) {
                    CircularProgressIndicator(
                        color = Tertiary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(40.dp),
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "Installing VEGAS\u2026",
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Extracting wrapper assets",
                        fontSize = 12.sp,
                        color = OnSurfaceVariant,
                    )
                }
            }
        }
    }

    // ── Error dialog ──
    errorMsg?.let { msg ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { errorMsg = null },
            title = {
                Text("Download Error", fontWeight = FontWeight.Bold, color = OnSurface)
            },
            text = { Text(msg, color = OnSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = { errorMsg = null }) {
                    Text("OK", color = Primary)
                }
            },
            containerColor = SurfaceColor,
        )
    }

    // ── Main dialog ──
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxSize(0.85f),
            shape = RoundedCornerShape(20.dp),
            color = SurfaceColor,
            tonalElevation = 6.dp,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ── Branded Header ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(PrimaryDim.copy(alpha = 0.25f), SurfaceColor)
                            )
                        )
                        .padding(start = 20.dp, end = 4.dp, top = 16.dp, bottom = 12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "VEGAS DOWNLOADS",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                brush = Brush.linearGradient(listOf(Primary, Secondary)),
                            )
                            Text(
                                text = "Select a release to download and install",
                                fontSize = 12.sp,
                                color = OnSurfaceVariant,
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = OnSurfaceVariant)
                        }
                    }
                }

                // Glow divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    GlowPurple.copy(alpha = 0.6f),
                                    GlowPurple.copy(alpha = 0.0f),
                                )
                            )
                        )
                )

                // ── Release List ──
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (isLoading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Primary)
                        }
                    } else if (releases.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                if (errorMsg != null) "Could not load releases." else "No releases available.",
                                color = OnSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 16.dp, vertical = 12.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(releases, key = { it.tagName }) { release ->
                                val isDownloading = downloadingTag == release.tagName
                                val versionNum = release.tagName.removePrefix("v")
                                val isInstalled = versionNum in installedVersions
                                val isRecommended = RECOMMENDED_TAG_PREFIXES.any { versionNum.startsWith(it) }
                                val isLatest = releases.firstOrNull()?.tagName == release.tagName

                                VegasReleaseCard(
                                    release = release,
                                    isDownloading = isDownloading,
                                    isInstalled = isInstalled,
                                    isRecommended = isRecommended,
                                    isLatest = isLatest,
                                    onDownload = {
                                        val url = release.wcpAssetUrl ?: return@VegasReleaseCard
                                        downloadingTag = release.tagName
                                        scope.launch {
                                            val uri = withContext(Dispatchers.IO) {
                                                downloadWcp(context, url, release.tagName)
                                            }
                                            downloadingTag = null
                                            if (uri != null) {
                                                installing = true
                                                installWcp(context, cm, uri) { ok, errorDetail ->
                                                    installing = false
                                                    if (ok) {
                                                        cm.syncContents()
                                                        onContentChanged()
                                                        onDismiss()
                                                    } else {
                                                        errorMsg = errorDetail ?: "Install failed."
                                                    }
                                                }
                                            } else {
                                                errorMsg = "Download failed."
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }

                // ── Footer ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = "Releases from isygold/vegas-releases",
                        fontSize = 10.sp,
                        color = OnSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun VegasReleaseCard(
    release: VegasRelease,
    isDownloading: Boolean,
    isInstalled: Boolean,
    isRecommended: Boolean,
    isLatest: Boolean,
    onDownload: () -> Unit,
) {
    val versionNum = release.tagName.removePrefix("v")

    androidx.compose.material3.Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceColor,
        tonalElevation = if (isRecommended) 4.dp else 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Title row with badges
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = release.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )

                    // "Latest" badge
                    if (isLatest) {
                        Box(
                            modifier = Modifier
                                .background(Tertiary.copy(alpha = 0.15f), RoundedCornerShape(3.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp),
                        ) {
                            Text(
                                "LATEST",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = Tertiary,
                            )
                        }
                    }

                    // "Recommended" badge
                    if (isRecommended && !isLatest) {
                        Box(
                            modifier = Modifier
                                .background(Secondary.copy(alpha = 0.15f), RoundedCornerShape(3.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp),
                        ) {
                            Text(
                                "RECOMMENDED",
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp,
                                color = Secondary,
                            )
                        }
                    }

                    // "Installed" badge
                    if (isInstalled) {
                        Box(
                            modifier = Modifier
                                .background(Primary.copy(alpha = 0.12f), RoundedCornerShape(3.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp),
                        ) {
                            Text(
                                "INSTALLED",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                color = Primary,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(3.dp))

                // Version + size
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = versionNum,
                        fontSize = 11.sp,
                        color = OnSurfaceVariant,
                    )
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .background(OnSurfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                    )
                    Text(
                        text = "VEGAS Wrapper",
                        fontSize = 11.sp,
                        color = GlowPurple,
                    )
                }
            }

            // Download button
            if (isInstalled) {
                Icon(
                    Icons.Filled.DownloadDone,
                    contentDescription = "Installed",
                    tint = Primary.copy(alpha = 0.5f),
                    modifier = Modifier.size(28.dp),
                )
            } else if (isDownloading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.5.dp,
                    color = Tertiary,
                )
            } else {
                IconButton(onClick = onDownload) {
                    Icon(
                        Icons.Filled.Download,
                        contentDescription = "Download",
                        tint = Primary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }

    // Subtle border glow for recommended
    if (isRecommended) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .padding(horizontal = 14.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Primary.copy(alpha = 0.3f),
                            GlowPurple.copy(alpha = 0.3f),
                            Color.Transparent,
                        )
                    )
                )
        )
    }
}

/** Download a .wcp file to cache dir and return a content:// URI. */
private fun downloadWcp(context: Context, url: String, tag: String): Uri? {
    val f = File(context.cacheDir, "vegas_${tag}.wcp")
    return if (Downloader.downloadFile(url, f)) Uri.fromFile(f) else null
}

/** Install a .wcp content package via ContentsManager. */
private fun installWcp(
    context: Context,
    cm: ContentsManager,
    uri: Uri,
    onDone: (success: Boolean, errorMsg: String?) -> Unit,
) {
    val activity = context as Activity
    Executors.newSingleThreadExecutor().execute {
        cm.extraContentFile(uri, object : ContentsManager.OnInstallFinishedCallback {
            var phase = 0
            override fun onFailed(reason: ContentsManager.InstallFailedReason, e: Exception?) {
                val msgId = when (reason) {
                    ContentsManager.InstallFailedReason.ERROR_BADTAR         -> R.string.file_cannot_be_recognied
                    ContentsManager.InstallFailedReason.ERROR_NOPROFILE      -> R.string.profile_not_found_in_content
                    ContentsManager.InstallFailedReason.ERROR_BADPROFILE     -> R.string.profile_cannot_be_recognized
                    ContentsManager.InstallFailedReason.ERROR_EXIST          -> R.string.content_already_exist
                    ContentsManager.InstallFailedReason.ERROR_MISSINGFILES   -> R.string.content_is_incomplete
                    ContentsManager.InstallFailedReason.ERROR_UNTRUSTPROFILE -> R.string.content_cannot_be_trusted
                    else                                                      -> R.string.unable_to_install_content
                }
                val detail = e?.message?.let { ": $it" } ?: ""
                activity.runOnUiThread {
                    onDone(false, "${context.getString(R.string.install_failed)}: ${context.getString(msgId)}$detail")
                }
            }
            override fun onSucceed(profile: ContentProfile) {
                if (phase == 0) {
                    phase = 1
                    cm.finishInstallContent(profile, this)
                } else {
                    activity.runOnUiThread { onDone(true, null) }
                }
            }
        })
    }
}

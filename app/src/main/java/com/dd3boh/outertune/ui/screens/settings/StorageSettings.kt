/*
 * Copyright (C) 2024 z-huang/InnerTune
 * Copyright (C) 2025 OuterTune Project
 *
 * SPDX-License-Identifier: GPL-3.0
 *
 * For any other attributions, refer to the git commit history
 */

package com.dd3boh.outertune.ui.screens.settings

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import com.dd3boh.outertune.LocalDatabase
import com.dd3boh.outertune.LocalDownloadUtil
import com.dd3boh.outertune.LocalPlayerAwareWindowInsets
import com.dd3boh.outertune.LocalPlayerConnection
import com.dd3boh.outertune.R
import com.dd3boh.outertune.constants.DownloadPathKey
import com.dd3boh.outertune.constants.ExcludedScanPathsKey
import com.dd3boh.outertune.constants.MaxImageCacheSizeKey
import com.dd3boh.outertune.constants.MaxSongCacheSizeKey
import com.dd3boh.outertune.constants.ScanPathsKey
import com.dd3boh.outertune.constants.SongSortType
import com.dd3boh.outertune.constants.ThumbnailCornerRadius
import com.dd3boh.outertune.constants.TopBarInsets
import com.dd3boh.outertune.extensions.tryOrNull
import com.dd3boh.outertune.ui.component.ActionPromptDialog
import com.dd3boh.outertune.ui.component.IconButton
import com.dd3boh.outertune.ui.component.InfoLabel
import com.dd3boh.outertune.ui.component.ListPreference
import com.dd3boh.outertune.ui.component.PreferenceEntry
import com.dd3boh.outertune.ui.component.PreferenceGroupTitle
import com.dd3boh.outertune.ui.utils.backToMain
import com.dd3boh.outertune.utils.formatFileSize
import com.dd3boh.outertune.utils.rememberPreference
import com.dd3boh.outertune.utils.scanners.stringFromUriList
import com.dd3boh.outertune.utils.scanners.uriListFromString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


@SuppressLint("PrivateResource")
@OptIn(ExperimentalCoilApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StorageSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val imageDiskCache = context.imageLoader.diskCache ?: return
    val playerCache = LocalPlayerConnection.current?.service?.playerCache ?: return
    val downloadCache = LocalPlayerConnection.current?.service?.downloadCache ?: return
    val database = LocalDatabase.current

    val coroutineScope = rememberCoroutineScope()

    val (downloadPath, onDownloadPathChange) = rememberPreference(DownloadPathKey, "")
    val (scanPaths, onScanPathsChange) = rememberPreference(ScanPathsKey, defaultValue = "")
    val (excludedScanPaths, onExcludedScanPathsChange) = rememberPreference(ExcludedScanPathsKey, defaultValue = "")

    var imageCacheSize by remember {
        mutableLongStateOf(imageDiskCache.size)
    }
    var playerCacheSize by remember {
        mutableLongStateOf(tryOrNull { playerCache.cacheSpace } ?: 0)
    }
    var downloadCacheSize by remember {
        mutableLongStateOf(tryOrNull { downloadCache.cacheSpace } ?: 0)
    }

    // download path selector
    val downloadUtil = LocalDownloadUtil.current
    var showDlPathDialog: Boolean by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(imageDiskCache) {
        while (isActive) {
            delay(500)
            imageCacheSize = imageDiskCache.size
        }
    }
    LaunchedEffect(playerCache) {
        while (isActive) {
            delay(500)
            playerCacheSize = tryOrNull { playerCache.cacheSpace } ?: 0
        }
    }
    LaunchedEffect(downloadCache) {
        while (isActive) {
            delay(500)
            downloadCacheSize = tryOrNull { downloadCache.cacheSpace } ?: 0
        }
    }

    val (maxImageCacheSize, onMaxImageCacheSizeChange) = rememberPreference(
        key = MaxImageCacheSizeKey,
        defaultValue = 512
    )
    val (maxSongCacheSize, onMaxSongCacheSizeChange) = rememberPreference(key = MaxSongCacheSizeKey, defaultValue = 0)


    // clear caches when turning off
    LaunchedEffect(maxImageCacheSize) {
        if (maxImageCacheSize == 0) {
            coroutineScope.launch(Dispatchers.IO) {
                imageDiskCache.clear()
            }
        }
    }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState())
    ) {
        PreferenceGroupTitle(
            title = stringResource(R.string.downloaded_songs)
        )

        Text(
            text = stringResource(R.string.size_used, formatFileSize(downloadCacheSize)),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        PreferenceEntry(
            title = { Text(stringResource(R.string.clear_all_downloads)) },
            onClick = {
                coroutineScope.launch(Dispatchers.IO) {
                    downloadCache.keys.forEach { key ->
                        downloadCache.removeResource(key)
                    }
                    database.downloadSongs(SongSortType.NAME, true).collect { songs ->
                        songs.forEach { song ->
                            downloadUtil.delete(song)
                        }
                    }
                }
            },
        )

        PreferenceEntry(
            title = { Text(stringResource(R.string.dl_main_path_title)) },
            onClick = {
                showDlPathDialog = true
            },
        )


        PreferenceGroupTitle(
            title = stringResource(R.string.song_cache)
        )

        if (maxSongCacheSize != 0) {
            if (maxSongCacheSize == -1) {
                Text(
                    text = stringResource(R.string.size_used, formatFileSize(playerCacheSize)),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            } else {
                LinearProgressIndicator(
                    progress = { (playerCacheSize.toFloat() / (maxSongCacheSize * 1024 * 1024L)).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                )

                Text(
                    text = stringResource(
                        R.string.size_used,
                        "${formatFileSize(playerCacheSize)} / ${formatFileSize(maxSongCacheSize * 1024 * 1024L)}"
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }

        ListPreference(
            title = { Text(stringResource(R.string.max_cache_size)) },
            selectedValue = maxSongCacheSize,
            values = listOf(0, 128, 256, 512, 1024, 2048, 4096, 8192, -1),
            valueText = {
                when (it) {
                    0 -> stringResource(androidx.compose.ui.R.string.state_off)
                    -1 -> stringResource(R.string.unlimited)
                    else -> formatFileSize(it * 1024 * 1024L)
                }
            },
            onValueSelected = onMaxSongCacheSizeChange
        )
        InfoLabel(stringResource(R.string.restart_to_apply_changes))

        PreferenceEntry(
            title = { Text(stringResource(R.string.clear_song_cache)) },
            onClick = {
                coroutineScope.launch(Dispatchers.IO) {
                    playerCache.keys.forEach { key ->
                        playerCache.removeResource(key)
                    }
                }
            },
        )

        PreferenceGroupTitle(
            title = stringResource(R.string.image_cache)
        )

        if (maxImageCacheSize > 0) {
            LinearProgressIndicator(
                progress = { (imageCacheSize.toFloat() / imageDiskCache.maxSize).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            Text(
                text = stringResource(
                    R.string.size_used,
                    "${formatFileSize(imageCacheSize)} / ${formatFileSize(imageDiskCache.maxSize)}"
                ),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        ListPreference(
            title = { Text(stringResource(R.string.max_cache_size)) },
            selectedValue = maxImageCacheSize,
            values = listOf(0, 128, 256, 512, 1024, 2048, 4096, 8192),
            valueText = {
                when (it) {
                    0 -> stringResource(androidx.compose.ui.R.string.state_off)
                    else -> formatFileSize(it * 1024 * 1024L)
                }
            },
            onValueSelected = onMaxImageCacheSizeChange
        )

        PreferenceEntry(
            title = { Text(stringResource(R.string.clear_image_cache)) },
            onClick = {
                coroutineScope.launch(Dispatchers.IO) {
                    imageDiskCache.clear()
                }
            },
        )
    }

    TopAppBar(
        title = { Text(stringResource(R.string.storage)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = null
                )
            }
        },
        windowInsets = TopBarInsets,
        scrollBehavior = scrollBehavior
    )


    if (showDlPathDialog) {
        var tempFilePath by remember {
            mutableStateOf<Uri?>(null)
        }
        LaunchedEffect(downloadPath) {
            tempFilePath = uriListFromString(downloadPath).firstOrNull()
        }

        ActionPromptDialog(
            titleBar = {
                Text(
                    text = stringResource(R.string.dl_main_path_title),
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            onDismiss = {
                showDlPathDialog = false
                tempFilePath = null
            },
            onConfirm = {
                tempFilePath?.let { f ->
                    val uris = stringFromUriList(listOfNotNull(f))
                    onDownloadPathChange(uris)
                }

                showDlPathDialog = false
                tempFilePath = null

                coroutineScope.launch {
                    delay(1000)
                    downloadUtil.cd()
                }
            },
            onReset = {
                tempFilePath = null
            },
            onCancel = {
                showDlPathDialog = false
                tempFilePath = null
            },
            isInputValid = uriListFromString(scanPaths).none {
                // download path cannot a scan path, or a subdir of a scan path
                tempFilePath.toString().length <= it.toString().length && tempFilePath.toString()
                    .contains(it.toString())
            }
        ) {

            val dirPickerLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocumentTree()
            ) { uri ->
                if (uri?.path != null) {
                    // Take persistable URI permission
                    val contentResolver = context.contentResolver
                    val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    contentResolver.takePersistableUriPermission(uri, takeFlags)

                    tempFilePath = uri
                }
            }

            val valid = uriListFromString(scanPaths).none {
                // download path cannot a scan path, or a subdir of a scan path
                tempFilePath.toString().length <= it.toString().length && tempFilePath.toString()
                    .contains(it.toString())
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(
                        2.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        RoundedCornerShape(ThumbnailCornerRadius)
                    )
                    .background(if (valid) Color.Transparent else MaterialTheme.colorScheme.errorContainer)
            ) {
                tempFilePath?.let {
                    Text(
                        text = it.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            // add folder button
            Column {
                Button(onClick = { dirPickerLauncher.launch(null) }) {
                    Text(stringResource(R.string.scan_paths_add_folder))
                }

                InfoLabel(
                    text = stringResource(R.string.scan_paths_tooltip),
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                if (!valid) {
                    InfoLabel(
                        text = stringResource(R.string.scanner_rejected_dir),
                        isError = true,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

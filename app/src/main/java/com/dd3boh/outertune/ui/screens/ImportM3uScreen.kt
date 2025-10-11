/*
 * Copyright (C) 2025 O​u​t​er​Tu​ne Project
 *
 * SPDX-License-Identifier: GPL-3.0
 *
 * For any other attributions, refer to the git commit history
 */
package com.dd3boh.outertune.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Input
import androidx.compose.material.icons.filled.Input
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Input
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.exoplayer.offline.Download
import androidx.navigation.NavController
import com.dd3boh.outertune.BuildConfig
import com.dd3boh.outertune.LocalDatabase
import com.dd3boh.outertune.LocalDownloadUtil
import com.dd3boh.outertune.LocalMenuState
import com.dd3boh.outertune.LocalPlayerAwareWindowInsets
import com.dd3boh.outertune.LocalPlayerConnection
import com.dd3boh.outertune.LocalSnackbarHostState
import com.dd3boh.outertune.R
import com.dd3boh.outertune.constants.AlbumThumbnailSize
import com.dd3boh.outertune.constants.ListThumbnailSize
import com.dd3boh.outertune.constants.ScannerM3uMatchCriteria
import com.dd3boh.outertune.constants.ThumbnailCornerRadius
import com.dd3boh.outertune.constants.TopBarInsets
import com.dd3boh.outertune.db.MusicDatabase
import com.dd3boh.outertune.db.entities.ArtistEntity
import com.dd3boh.outertune.db.entities.Song
import com.dd3boh.outertune.db.entities.SongEntity
import com.dd3boh.outertune.extensions.move
import com.dd3boh.outertune.extensions.togglePlayPause
import com.dd3boh.outertune.models.toMediaMetadata
import com.dd3boh.outertune.playback.queues.ListQueue
import com.dd3boh.outertune.ui.component.AutoResizeText
import com.dd3boh.outertune.ui.component.ChipsLazyRow
import com.dd3boh.outertune.ui.component.ChipsRow
import com.dd3boh.outertune.ui.component.EnumListPreference
import com.dd3boh.outertune.ui.component.FontSizeRange
import com.dd3boh.outertune.ui.component.LazyColumnScrollbar
import com.dd3boh.outertune.ui.component.button.IconButton
import com.dd3boh.outertune.ui.component.button.IconTextButton
import com.dd3boh.outertune.ui.component.items.Icon
import com.dd3boh.outertune.ui.component.items.ItemThumbnail
import com.dd3boh.outertune.ui.component.items.ListItem
import com.dd3boh.outertune.ui.dialog.AddToPlaylistDialog
import com.dd3boh.outertune.ui.dialog.DefaultDialog
import com.dd3boh.outertune.ui.menu.SongMenu
import com.dd3boh.outertune.ui.screens.Screens.LibraryFilter
import com.dd3boh.outertune.ui.screens.playlist.PlaylistType
import com.dd3boh.outertune.ui.utils.backToMain
import com.dd3boh.outertune.ui.utils.getNSongsString
import com.dd3boh.outertune.utils.joinByBullet
import com.dd3boh.outertune.utils.lmScannerCoroutine
import com.dd3boh.outertune.utils.makeTimeString
import com.dd3boh.outertune.utils.reportException
import com.dd3boh.outertune.utils.scanners.LocalMediaScanner
import com.dd3boh.outertune.utils.scanners.LocalMediaScanner.Companion.compareM3uSong
import com.dd3boh.outertune.viewmodels.ImportM3uViewModel
import com.dd3boh.outertune.viewmodels.LocalFilter
import com.zionhuang.innertube.YouTube
import com.zionhuang.innertube.models.SongItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.io.InputStream
import java.util.UUID
import kotlin.collections.distinctBy
import kotlin.collections.first
import kotlin.collections.orEmpty
import kotlin.text.contains
import kotlin.text.startsWith
import kotlin.text.substringAfter
import kotlin.text.substringBefore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportM3uScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    replaceSong: MutableState<Song?>,
    viewModel: ImportM3uViewModel = viewModel ()
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val snackbarHostState = LocalSnackbarHostState.current

    var scannerSensitivity by rememberSaveable {
        mutableStateOf(ScannerM3uMatchCriteria.LEVEL_1)
    }

    var remoteLookup by rememberSaveable { mutableStateOf(false) }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }
    var importedTitle by rememberSaveable { mutableStateOf("") }
    var searchId by rememberSaveable { mutableIntStateOf(0) }

    val mainListState = rememberSaveable(saver = LazyListState.Saver) {
        LazyListState()
    }

    val headerItems = 2
    val reorderableState = rememberReorderableLazyListState(
        lazyListState = mainListState,
        scrollThresholdPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
    ) { from, to ->
        if (to.index >= headerItems && from.index >= headerItems) {
            viewModel.importedSongs.move(from.index - headerItems, to.index - headerItems)
        }
    }

    var isSearching by rememberSaveable { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    var searchQuery by rememberSaveable { mutableStateOf("") }

    val playerConnection = LocalPlayerConnection.current

    var percentage by rememberSaveable { mutableIntStateOf(0) }

    var showOptions by remember { mutableStateOf(false) }


    val haptic = LocalHapticFeedback.current

    var importedChipsValue by remember {mutableStateOf(importM3uFilter.ALL)}

    val title =  stringResource(R.string.import_playlist)

    var showEditOptions by remember { mutableStateOf(false) }

    val importJob = remember { SupervisorJob() }

    val importScope = remember {
        CoroutineScope(importJob + Dispatchers.IO)
    }

    DisposableEffect(Unit) {
        onDispose {
            importJob.cancel()
        }
    }

    val importM3uLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        isLoading = true
        importScope.launch {
            try {
                if (uri != null) {
                    viewModel.importedSongs.clear()
                    viewModel.rejectedSongs.clear()

                    val result = loadM3u(
                        context = context,
                        database = database,
                        snackbarHostState = snackbarHostState,
                        uri = uri,
                        matchStrength = scannerSensitivity,
                        searchOnline = remoteLookup,
                        onPercentageChange = { newVal ->
                            percentage = newVal
                        }
                    )
                    viewModel.importedSongs.addAll(result.first)
                    viewModel.rejectedSongs.addAll(result.second)
                }
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(replaceSong.value) {
        if (replaceSong.value != null) {
            val prevSongQuery = viewModel.importedSongs[searchId].first
            viewModel.importedSongs[searchId] =
                Triple(prevSongQuery, replaceSong.value, UUID.randomUUID().toString()) as Triple<String, Song, String>
            replaceSong.value = null
        }
    }

    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
        }
    }

    val padding = LocalPlayerAwareWindowInsets.current.asPaddingValues()

    if (isSearching) {
        BackHandler {
            isSearching = false
        }
    }

    if (showOptions) {
        DefaultDialog(
            onDismiss = { showOptions = false },
            icon = { Icon(Icons.Rounded.Settings, null) },
            title = { Text(stringResource(R.string.settings)) },
        ) {
            EnumListPreference(
                title = { Text(stringResource(R.string.scanner_sensitivity_title)) },
                icon = { Icon(Icons.Rounded.GraphicEq, null) },
                selectedValue = scannerSensitivity,
                onValueSelected = { scannerSensitivity = it },
                valueText = {
                    when (it) {
                        ScannerM3uMatchCriteria.LEVEL_0 -> stringResource(R.string.scanner_sensitivity_L0)
                        ScannerM3uMatchCriteria.LEVEL_1 -> stringResource(R.string.scanner_sensitivity_L1)
                        ScannerM3uMatchCriteria.LEVEL_2 -> stringResource(R.string.scanner_sensitivity_L2)
                    }
                }
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = remoteLookup,
                    onCheckedChange = { remoteLookup = it }
                )
                Text(
                    stringResource(R.string.m3u_ytm_lookup), color = MaterialTheme.colorScheme.secondary,
                    fontSize = 14.sp
                )
            }
        }
    }

    if (showEditOptions) {
        DefaultDialog(
            onDismiss = { showEditOptions = false },
            title = { Text(stringResource(R.string.options)) },
        ) {
            Column () {
                TextButton(
                    onClick = {
                        viewModel.importedSongs.removeAt(searchId)
                        showEditOptions = false
                    },
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = null
                    )
                    Text(stringResource(R.string.delete))
                }
                TextButton(
                    onClick = {
                        val route =
                            "search_rep/${Uri.encode(viewModel.importedSongs.map { (query, _) -> query }[searchId])}?rep=true"
                        navController.navigate(route)
                        showEditOptions = false
                    },
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SwapHoriz,
                        contentDescription = null
                    )
                    Text(stringResource(R.string.edit))
                }
            }
        }
    }

    Column (
        modifier = Modifier
            .fillMaxSize()
    ){
        TopAppBar(
            title = {
                if (isSearching) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.search),
                                style = MaterialTheme.typography.titleLarge
                            )
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleLarge,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                }
            },
            actions = {
                if (!isSearching) {
                    IconButton(
                        onClick = {
                            isSearching = true
                        }
                    ) {
                        Icon(
                            Icons.Rounded.Search,
                            contentDescription = null
                        )
                    }
                }
            },
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

        LazyColumn(
            state = mainListState,
            contentPadding = PaddingValues(
                start = padding.calculateStartPadding(LayoutDirection.Ltr),
                top = 0.dp,
                bottom = padding.calculateBottomPadding(),
                end = padding.calculateEndPadding(LayoutDirection.Ltr)
            ),
            modifier = Modifier
                .fillMaxWidth()
        ) {
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(AlbumThumbnailSize)
                                .background(
                                    MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
                                    shape = RoundedCornerShape(ThumbnailCornerRadius)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Input,
                                contentDescription = null,
                                tint = LocalContentColor.current.copy(alpha = 0.8f),
                                modifier = Modifier
                                    .size(AlbumThumbnailSize / 2 + 16.dp)
                                    .align(Alignment.Center)
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.Start
                        ) {
                            AutoResizeText(
                                text = title,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                fontSizeRange = FontSizeRange(16.sp, 22.sp)
                            )
                            TextButton(
                                onClick = { showOptions = true },
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Settings,
                                    contentDescription = null
                                )
                                Text(stringResource(R.string.settings))
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Button(
                            onClick = { showChoosePlaylistDialog = true },
                            enabled = viewModel.importedSongs.isNotEmpty()
                        ) {
                            Text(stringResource(R.string.add_to_playlist))
                        }

                        Spacer(Modifier.width(8.dp))

                        Button(
                            onClick = {
                                importM3uLauncher.launch(arrayOf("audio/*"))
                            },
                            enabled = !isLoading
                        ) {
                            Text("import m3u") // TODO: add to R.string
                        }

                    }

                    if (viewModel.importedSongs.isNotEmpty() || viewModel.rejectedSongs.isNotEmpty()) {
                        ChipsRow(
                            chips = listOf(
                                importM3uFilter.ALL to "${stringResource(R.string.filter_all)} (${viewModel.importedSongs.size + viewModel.rejectedSongs.size})",
                                importM3uFilter.IMPORTED to "Imported (${viewModel.importedSongs.size})",
                                importM3uFilter.REJECTED to "Rejected (${viewModel.rejectedSongs.size})",
                            ),
                            currentValue = importedChipsValue,
                            onValueUpdate = { importedChipsValue = it }
                        )
                    }

                }
            }

            if (isLoading) {
                item {
                    Box (
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 150.dp)

                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(80.dp)
                        ) {
                            Text(
                                text = "$percentage%",
                                fontSize = 24.sp,
                            )
                            CircularProgressIndicator(
                                strokeWidth = 4.dp,
                                modifier = Modifier.size(120.dp)
                            )
                        }
                    }
                }
            } else if (viewModel.importedSongs.isEmpty()) {
                item {
                    Column (
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 120.dp)

                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(AlbumThumbnailSize/2)
                                .padding(4.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
                                    shape = RoundedCornerShape(ThumbnailCornerRadius)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = LocalContentColor.current.copy(alpha = 0.8f),
                                modifier = Modifier
                                    .size(AlbumThumbnailSize / 4 + 16.dp)
                                    .align(Alignment.Center)
                            )
                        }
                        Text(
                            text = "Import a playlist to get\nstarted", // TODO: add to R.string
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            if (viewModel.importedSongs.isNotEmpty()) {
                itemsIndexed(
                    items = viewModel.importedSongs.map { (_, song, uuid) -> Pair(song, uuid) },
                    key = { _, (_, uuid) -> uuid }
                ) { index, (song, uuid) ->
                    if (isSearching && !queryMatchesSong(searchQuery, song)) {
                        return@itemsIndexed
                    }
                    ReorderableItem(
                        state = reorderableState,
                        key = uuid,
                        enabled = true
                    ) {
                        ListItem(
                            title = song.song.title,
                            subtitle = joinByBullet(
                                (if (BuildConfig.DEBUG) song.song.id else ""),
                                song.artists.joinToString { it.name },
                                makeTimeString(song.song.duration * 1000L)
                            ),
                            badges = {
                                if (song.song.liked) {
                                    Icon.Favorite()
                                }
                                if (song.song.isLocal) {
                                    Icon.FolderCopy()
                                } else if (song.song.inLibrary != null) {
                                    Icon.Library()
                                }
                                if (LocalDownloadUtil.current.getCustomDownload(song.id)) {
                                    Icon.Download(Download.STATE_COMPLETED)
                                } else {
                                    val download by LocalDownloadUtil.current.getDownload(song.id)
                                        .collectAsState(initial = null)
                                    Icon.Download(download?.state)
                                }
                            },
                            thumbnailContent = {
                                ItemThumbnail(
                                    thumbnailUrl = if (song.song.isLocal) song.song.localPath else song.song.thumbnailUrl,
                                    isActive = false,
                                    isPlaying = false,
                                    shape = RoundedCornerShape(ThumbnailCornerRadius),
                                    modifier = Modifier.size(ListThumbnailSize)
                                )
                            },
                            trailingContent = {
                                IconButton(
                                    onClick = {
                                        showEditOptions = true
                                        searchId = index
                                        haptic.performHapticFeedback(HapticFeedbackType.Companion.ContextClick)
                                    }
                                ) {
                                    Icon(
                                        Icons.Rounded.MoreVert,
                                        contentDescription = null
                                    )
                                }
                                IconButton(
                                    onClick = {},
                                    modifier = Modifier.draggableHandle()
                                ) {
                                    Icon(
                                        Icons.Rounded.DragHandle,
                                        contentDescription = null
                                    )
                                }

                            },
                            isSelected = false,
                            isActive = LocalPlayerConnection.current?.mediaMetadata?.collectAsState()?.value?.id == song.id,
                            modifier = Modifier.combinedClickable(
                                onClick = {
                                    if (song.id == playerConnection?.mediaMetadata?.value?.id) {
                                        playerConnection.player.togglePlayPause()
                                    } else {
                                        playerConnection?.playQueue(
                                            ListQueue(
                                                items = List(1) { song.toMediaMetadata() },
                                                startIndex = 0,
                                            )
                                        )
                                    }
                                },
                            )
                        )
                    }
                }
                if (viewModel.rejectedSongs.isNotEmpty()) {
                    itemsIndexed(
                        items = viewModel.rejectedSongs.map { (_, song) -> song },
                        key = { index, song -> index.hashCode() + song.hashCode() }
                    ) { index, song ->
                        if (isSearching && !queryMatchesSong(searchQuery, song)) {
                            return@itemsIndexed
                        }
                        ListItem(
                            title = song.song.title,
                            subtitle = joinByBullet(
                                (if (BuildConfig.DEBUG) song.song.id else ""),
                                song.artists.joinToString { it.name },
                                makeTimeString(song.song.duration * 1000L)
                            ),
                            thumbnailContent = {},
                            trailingContent = {},
                            isSelected = false,
                            isActive = false,
                        )
                    }
                }
            }
        }

        LazyColumnScrollbar(
            state = mainListState,
        )
    }

    if (showChoosePlaylistDialog) {
        AddToPlaylistDialog(
            navController = navController,
            allowSyncing = false,
            initialTextFieldValue = importedTitle,
            songIds = viewModel.importedSongs.map { (_, song) -> song.id },
            onPreAdd = {
                viewModel.importedSongs.map { (_, song) -> song }.forEach {
                    database.insert(it.toMediaMetadata())
                }
                emptyList()
            },
            onDismiss = { showChoosePlaylistDialog = false }
        )
    }
}


/**
 * Parse m3u file and scans the database for matching songs
 *
 * @param uri Uri for m3u file
 * @param matchStrength How lax should the scanner be
 * @param searchOnline Whether to enable fallback for trying to find the song on YTM
 */
suspend fun loadM3u(
    context: Context,
    database: MusicDatabase,
    snackbarHostState: SnackbarHostState,
    uri: Uri,
    matchStrength: ScannerM3uMatchCriteria = ScannerM3uMatchCriteria.LEVEL_1,
    searchOnline: Boolean = false,
    onPercentageChange: (Int) -> Unit
): Triple<ArrayList<Triple<String, Song, String>>, ArrayList<Pair<Int, Song>>, String> = withContext(Dispatchers.IO) {
    val unorderedSongs = ArrayList<Triple<Int, String, Song>>()
    val unorderedRejectedSongs = ArrayList<Pair<Int, Song>>()

    var songs = ArrayList<Triple<String, Song, String>>()
    var rejectedSongs = ArrayList<Pair<Int, Song>>()
    var toProcess: Int
    var processed = 0

    runCatching {
        context.applicationContext.contentResolver.openInputStream(uri)?.use { stream ->
            val lines = stream.readLines()
            if (lines.isEmpty()) return@runCatching
            if (lines.first().startsWith("#EXTM3U")) {
                toProcess = lines.size / 2
                coroutineScope {
                    lines.forEachIndexed { index, rawLine ->
                        launch {
                            if (rawLine.startsWith("#EXTINF:")) {
                                val artists =
                                    rawLine.substringAfter("#EXTINF:").substringAfter(',')
                                        .substringBefore(" - ").split(';')
                                val title = rawLine.substringAfter("#EXTINF:").substringAfter(',')
                                    .substringAfter(" - ")
                                val source = if (index + 1 < lines.size) lines[index + 1] else null

                                val mockSong = Song(
                                    song = SongEntity(
                                        id = "",
                                        title = title,
                                        isLocal = true,
                                        localPath = if (source?.startsWith("http") == false) source.substringAfter(
                                            ','
                                        ) else null
                                    ),
                                    artists = artists.map { ArtistEntity("", it) },
                                )

                                // now find the best match
                                // first, search for songs in the database. Supplement with remote songs if no results are found
                                val matches = if (source == null) {
                                    database.searchSongsInDb(title).first().toMutableList()
                                } else {
                                    // local songs have a source format of "<id>, <path>", YTM songs have "<url>
                                    var id = source.substringBefore(',')
                                    if (id.isEmpty()) {
                                        id = source.substringAfter("watch?").substringAfter("=")
                                            .substringBefore('?')
                                    }
                                    val dbResult = mutableListOf(database.song(id).first())
                                    dbResult.addAll(database.searchSongsInDb(title).first())
                                    dbResult.filterNotNull().toMutableList()
                                }
                                // do not search for local songs
                                val query = "$title ${Uri.decode(artists.joinToString(" "))}"
                                if (searchOnline && matches.isEmpty() && source?.contains(',') == false) {
                                    val suggestions = YouTube.searchSuggestions(query).getOrNull()
                                    val suggestionSongs = suggestions?.recommendedItems.orEmpty().distinctBy { it.id }.filter { it is SongItem }
                                    suggestionSongs.forEach { suggestion ->
                                        val song = (suggestion as SongItem).toMediaMetadata()
                                        val result = Song(
                                            song = song.toSongEntity(),
                                            artists = song.artists.map {
                                                ArtistEntity(
                                                    id = it.id ?: ArtistEntity.generateArtistId(),
                                                    name = it.name
                                                )
                                            }
                                        )
                                        matches.add(result)
                                    }
                                    val onlineResult =
                                        LocalMediaScanner.youtubeSongLookup(query, source)
                                    onlineResult.forEach { result ->
                                        val result = Song(
                                            song = result.toSongEntity(),
                                            artists = result.artists.map {
                                                ArtistEntity(
                                                    id = it.id ?: ArtistEntity.generateArtistId(),
                                                    name = it.name
                                                )
                                            }
                                        )
                                        matches.add(result)
                                    }
                                }
                                matches.distinctBy { it.id }
                                val oldSize = unorderedSongs.size
                                var foundOne =
                                    false // TODO: Eventually the user can pick from matches... eventually...

                                // take first song when searching on YTM
                                if (matchStrength == ScannerM3uMatchCriteria.LEVEL_0 && searchOnline && matches.isNotEmpty()) {
                                    unorderedSongs.add(Triple(index, query, matches.first()))
                                } else {
                                    for (s in matches) {
                                        if (compareM3uSong(
                                                mockSong,
                                                s,
                                                matchStrength = matchStrength
                                            )
                                        ) {
                                            unorderedSongs.add(Triple(index, "", s))
                                            foundOne = true
                                            break
                                        }
                                    }
                                }

                                if (oldSize == unorderedSongs.size) {
                                    unorderedRejectedSongs.add(Pair(index, mockSong))
                                }
                                processed++
                                val percent = if (toProcess > 0)
                                    ((processed.toFloat() / toProcess) * 100).toInt()
                                else 0
                                onPercentageChange(percent)
                            }
                        }
                    }
                }
                while (processed < toProcess) {
                    delay(10)
                }
                unorderedSongs.sortBy { it.first }
                unorderedRejectedSongs.sortBy { it.first }
                songs = unorderedSongs.map { (_, query, song) -> Triple(query, song, UUID.randomUUID().toString()) } as ArrayList<Triple<String, Song, String>>
                rejectedSongs = unorderedRejectedSongs
                onPercentageChange(0)
            }
        }
    }.onFailure {
        if (it !is CancellationException){
            reportException(it)
            Toast.makeText(context, R.string.m3u_import_playlist_failed, Toast.LENGTH_SHORT).show()
        }
    }

    if (songs.isEmpty()) {
        withContext(Dispatchers.Main) {
            snackbarHostState.showSnackbar(
                message = context.getString(R.string.m3u_import_failed),
                withDismissAction = true,
                duration = SnackbarDuration.Long
            )
        }
    }
    var name: String? = null
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) {
                name = it.getString(nameIndex)
            }
        }
    }
    val fileName = (name ?: (uri.path?.substringAfterLast('/') ?: "")).substringBeforeLast('.')
    Triple(songs, rejectedSongs, fileName)
}


/**
 * Read a file to a string
 */
fun InputStream.readLines(): List<String> {
    return this.bufferedReader().useLines { it.toList() }
}

fun queryMatchesSong(query: String, song: Song) : Boolean{
    val songData = "${song.title} ${Uri.decode(song.artists.joinToString( " "))}"
    return songData.lowercase().contains(query.lowercase())
}

enum class importM3uFilter {
    ALL, IMPORTED, REJECTED
}
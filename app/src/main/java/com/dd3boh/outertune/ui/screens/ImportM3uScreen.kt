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
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dd3boh.outertune.LocalDatabase
import com.dd3boh.outertune.LocalMenuState
import com.dd3boh.outertune.LocalPlayerAwareWindowInsets
import com.dd3boh.outertune.LocalPlayerConnection
import com.dd3boh.outertune.LocalSnackbarHostState
import com.dd3boh.outertune.R
import com.dd3boh.outertune.constants.AlbumThumbnailSize
import com.dd3boh.outertune.constants.ScannerM3uMatchCriteria
import com.dd3boh.outertune.constants.SearchSource
import com.dd3boh.outertune.constants.SearchSourceKey
import com.dd3boh.outertune.constants.ThumbnailCornerRadius
import com.dd3boh.outertune.constants.TopBarInsets
import com.dd3boh.outertune.db.MusicDatabase
import com.dd3boh.outertune.db.entities.ArtistEntity
import com.dd3boh.outertune.db.entities.Song
import com.dd3boh.outertune.db.entities.SongEntity
import com.dd3boh.outertune.extensions.move
import com.dd3boh.outertune.extensions.togglePlayPause
import com.dd3boh.outertune.models.MediaMetadata
import com.dd3boh.outertune.models.toMediaMetadata
import com.dd3boh.outertune.playback.queues.ListQueue
import com.dd3boh.outertune.ui.component.ChipsRow
import com.dd3boh.outertune.ui.component.EmptyPlaceholder
import com.dd3boh.outertune.ui.component.EnumListPreference
import com.dd3boh.outertune.ui.component.FloatingFooter
import com.dd3boh.outertune.ui.component.LazyColumnScrollbar
import com.dd3boh.outertune.ui.component.SearchBar
import com.dd3boh.outertune.ui.component.SelectHeader
import com.dd3boh.outertune.ui.component.SelectHeaderM3u
import com.dd3boh.outertune.ui.component.button.IconButton
import com.dd3boh.outertune.ui.component.items.M3uSongListItem
import com.dd3boh.outertune.ui.component.items.M3uSongSearchListItem
import com.dd3boh.outertune.ui.component.items.YouTubeListItem
import com.dd3boh.outertune.ui.component.shimmer.ListItemPlaceHolder
import com.dd3boh.outertune.ui.component.shimmer.ShimmerHost
import com.dd3boh.outertune.ui.dialog.AddToPlaylistDialog
import com.dd3boh.outertune.ui.dialog.DefaultDialog
import com.dd3boh.outertune.ui.menu.ImportSongMenu
import com.dd3boh.outertune.ui.utils.backToMain
import com.dd3boh.outertune.utils.lmScannerCoroutine
import com.dd3boh.outertune.utils.rememberEnumPreference
import com.dd3boh.outertune.utils.reportException
import com.dd3boh.outertune.utils.scanners.LocalMediaScanner
import com.dd3boh.outertune.utils.scanners.LocalMediaScanner.Companion.compareM3uSong
import com.dd3boh.outertune.viewmodels.ImportM3uViewModel
import com.dd3boh.outertune.viewmodels.ImportedSong
import com.zionhuang.innertube.YouTube
import com.zionhuang.innertube.models.SongItem
import com.zionhuang.innertube.models.YTItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
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

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun ImportM3uScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: ImportM3uViewModel = hiltViewModel()
) {
    val redError = Color(0.949f, 0.188f, 0.133f)
    val orangeWarning = Color(0.961f, 0.682f, 0.443f)

    val context = LocalContext.current
    val database = LocalDatabase.current
    val focusRequester = remember { FocusRequester() }
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current
    val snackbarHostState = LocalSnackbarHostState.current
    val windowInsets = LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime)

    var scannerSensitivity by rememberSaveable {
        mutableStateOf(ScannerM3uMatchCriteria.LEVEL_1)
    }
    var searchSource by rememberEnumPreference(SearchSourceKey, SearchSource.ONLINE)

    var remoteLookup by rememberSaveable { mutableStateOf(false) }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }
    var showExitConfirm by rememberSaveable {
        mutableStateOf(false)
    }
    var importedTitle by rememberSaveable { mutableStateOf("") }
    var searchId by rememberSaveable { mutableIntStateOf(0) }

    val mainListState = rememberSaveable(saver = LazyListState.Saver) {
        LazyListState()
    }

    val headerItems = 1
    val reorderableState = rememberReorderableLazyListState(
        lazyListState = mainListState,
        scrollThresholdPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
    ) { from, to ->
        if (to.index >= headerItems && from.index >= headerItems) {
            viewModel.importedSongs.move(from.index - headerItems, to.index - headerItems)
        }
    }

    // TODO: m3u: future selection mode
    var inSelectMode by rememberSaveable { mutableStateOf(false) }
    val selection = rememberSaveable(
        saver = listSaver<MutableList<String>, String>(
            save = { it.toList() },
            restore = { it.toMutableStateList() }
        )
    ) { mutableStateListOf() }
    val onExitSelectionMode = {
        inSelectMode = false
        selection.clear()
    }

    val navigationItems = listOf(Screens.M3uList, Screens.M3uSearch)
    val m3uNavController = rememberNavController()
    val navBackStackEntry by m3uNavController.currentBackStackEntryAsState()

    // search
    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }

    val (searchQuery, onSearchQueryChange) = rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }

    LaunchedEffect(searchQuery) {
        snapshotFlow { searchQuery }.debounce { 300L }.collectLatest {
            if (searchSource == SearchSource.ONLINE) {
                viewModel.search(searchQuery.text)
            } else {
                viewModel.query.value = searchQuery.text
            }
        }
    }

    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
        }
    }

    var percentage by rememberSaveable { mutableIntStateOf(0) }


    val haptic = LocalHapticFeedback.current

    var importedChipsValue by remember { mutableStateOf(ImportM3uFilter.ALL) }

    val importJob = remember { SupervisorJob() }

    val importScope = remember {
        CoroutineScope(importJob + lmScannerCoroutine)
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
                    importedTitle = result.second
                }
            } finally {
                isLoading = false
            }
        }
    }

    fun handleBack() {
        if (navBackStackEntry?.destination?.route?.let { (it == Screens.M3uSearch.route) } == true) {
            onSearchQueryChange(TextFieldValue())
            m3uNavController.navigate(Screens.M3uList.route)
        } else if (isSearching) {
            isSearching = false
            query = TextFieldValue()
        } else if (inSelectMode) {
            onExitSelectionMode()
        } else {
            showExitConfirm = true
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        NavHost(
            navController = m3uNavController,
            startDestination = Screens.M3uList.route,
            enterTransition = {
                val currentRouteIndex = navigationItems.indexOfFirst {
                    it.route == targetState.destination.route
                }
                val previousRouteIndex = navigationItems.indexOfFirst {
                    it.route == initialState.destination.route
                }

                if (currentRouteIndex == -1 || currentRouteIndex > previousRouteIndex)
                    slideInHorizontally { it / 2 } + fadeIn(tween(250))
                else
                    slideInHorizontally { -it / 2 } + fadeIn(tween(250))
            },
            exitTransition = {
                val currentRouteIndex = navigationItems.indexOfFirst {
                    it.route == initialState.destination.route
                }
                val targetRouteIndex = navigationItems.indexOfFirst {
                    it.route == targetState.destination.route
                }

                if (targetRouteIndex == -1 || targetRouteIndex > currentRouteIndex)
                    slideOutHorizontally { -it / 2 } + fadeOut(tween(250))
                else
                    slideOutHorizontally { it / 2 } + fadeOut(tween(250))
            },
            popEnterTransition = {
                val currentRouteIndex = navigationItems.indexOfFirst {
                    it.route == targetState.destination.route
                }
                val previousRouteIndex = navigationItems.indexOfFirst {
                    it.route == initialState.destination.route
                }

                if (previousRouteIndex != -1 && previousRouteIndex < currentRouteIndex)
                    slideInHorizontally { it / 2 } + fadeIn(tween(250))
                else
                    slideInHorizontally { -it / 2 } + fadeIn(tween(250))
            },
            popExitTransition = {
                val currentRouteIndex = navigationItems.indexOfFirst {
                    it.route == initialState.destination.route
                }
                val targetRouteIndex = navigationItems.indexOfFirst {
                    it.route == targetState.destination.route
                }

                if (currentRouteIndex != -1 && currentRouteIndex < targetRouteIndex)
                    slideOutHorizontally { -it / 2 } + fadeOut(tween(250))
                else
                    slideOutHorizontally { it / 2 } + fadeOut(tween(250))
            },
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
        ) {
            composable(Screens.M3uList.route) {
                BackHandler {
                    if (isSearching) {
                        isSearching = false
                        query = TextFieldValue()
                    } else if (inSelectMode) {
                        onExitSelectionMode()
                    } else {
                            showExitConfirm = true
                    }
                }
                LazyColumn(
                    state = mainListState,
                    contentPadding = windowInsets.asPaddingValues()
                ) {
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
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
                                    stringResource(R.string.m3u_ytm_lookup),
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontSize = 14.sp
                                )
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
                                    Text(stringResource(R.string.import_m3u))
                                }
                            }

                            if (viewModel.importedSongs.isNotEmpty()) {
                                ChipsRow(
                                    chips = listOf(
                                        ImportM3uFilter.ALL to stringResource(R.string.filter_all_imported_songs, viewModel.importedSongs.size),
                                        ImportM3uFilter.IMPORTED to stringResource(R.string.filter_imported, viewModel.importedSongs.filter { it.status == ImportM3uFilter.IMPORTED || it.status == ImportM3uFilter.MISMATCH}.size),
                                        ImportM3uFilter.MISSING to stringResource(R.string.filter_missing, viewModel.importedSongs.filter { it.status == ImportM3uFilter.MISSING }.size),
                                        ImportM3uFilter.MISMATCH to stringResource(R.string.filter_mismatch, viewModel.importedSongs.filter { it.status == ImportM3uFilter.MISMATCH }.size),
                                    ),
                                    currentValue = importedChipsValue,
                                    onValueUpdate = { importedChipsValue = it }
                                )
                            }
                        }
                    }

                    if (isLoading) {
                        item {
                            Box(
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
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = 120.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(AlbumThumbnailSize / 2)
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
                                    text = stringResource(R.string.import_playlist_to_get_started),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    if (viewModel.importedSongs.isNotEmpty()) {
                        val songs = viewModel.importedSongs
                            .filter { !isSearching || queryMatchesSong(query.text, it.song) }
                            .filter {
                                when (importedChipsValue) {
                                    ImportM3uFilter.IMPORTED -> it.status == ImportM3uFilter.IMPORTED || it.status == ImportM3uFilter.MISMATCH
                                    ImportM3uFilter.MISSING -> it.status == ImportM3uFilter.MISSING
                                    ImportM3uFilter.MISMATCH -> it.status == ImportM3uFilter.MISMATCH
                                    else -> true
                                }
                            }
                        itemsIndexed(
                            items = songs,
                            key = { _, (_, _, uuid, _) -> uuid }
                        ) { index, (query, song, uuid, status) ->
                            ReorderableItem(
                                state = reorderableState,
                                key = uuid,
                            ) {
                                M3uSongListItem(
                                    song = song,
                                    isMissing = status == ImportM3uFilter.MISSING,
                                    inSelectMode = inSelectMode,
                                    isSelected = selection.contains(uuid),
                                    onSelectedChange = { selected ->
                                        if (selected) {
                                            selection.add(uuid)
                                        } else {
                                            selection.remove(uuid)
                                        }
                                    },
                                    onEditClick = {
                                        menuState.show {
                                            ImportSongMenu (
                                                song = song,
                                                modelIndex = Pair(viewModel, index),
                                                navController = navController,
                                                m3uNavController = m3uNavController,
                                                onDismiss = menuState::dismiss
                                            )
                                        }
                                        searchId = index
                                        haptic.performHapticFeedback(HapticFeedbackType.Companion.ContextClick)
                                        onSearchQueryChange(TextFieldValue(query))
                                    },
                                    modifier = Modifier
                                        .background (
                                            when (status) {
                                                ImportM3uFilter.MISSING -> redError         // default MaterialTheme looked like shi
                                                ImportM3uFilter.MISMATCH -> orangeWarning
                                                else ->  MaterialTheme.colorScheme.background
                                            }
                                        )
                                        .combinedClickable(
                                            onClick = {
                                                if (inSelectMode) {
                                                    if (selection.contains(uuid)) {
                                                        selection.remove(uuid)
                                                    } else {
                                                        selection.add(uuid)
                                                    }
                                                } else if (status == ImportM3uFilter.IMPORTED || status == ImportM3uFilter.MISMATCH) {
                                                    if (song.id == playerConnection?.mediaMetadata?.value?.id) {
                                                        playerConnection.player.togglePlayPause()
                                                    } else {
                                                        playerConnection?.playQueue(
                                                            ListQueue(
                                                                items = listOf(song.toMediaMetadata()),
                                                                startIndex = 0,
                                                            )
                                                        )
                                                    }
                                                }
                                            },
                                            onLongClick = {
                                                if (!inSelectMode) {
                                                    inSelectMode = true
                                                    selection.add(uuid)
                                                }
                                            }
                                        )

                                )
                            }
                        }
                    }
                }
            }
            composable(Screens.M3uSearch.route) {
                val searchBarFocusRequester = remember { FocusRequester() }

                SearchBar(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    onSearch = {
                        onSearchQueryChange(TextFieldValue(it))
                    },
                    active = true,
                    onActiveChange = { notShouldExit ->
                        if (!notShouldExit) handleBack()
                    },
                    scrollBehavior = scrollBehavior,
                    placeholder = {
                        Text(
                            text = stringResource(
                                when (searchSource) {
                                    SearchSource.LOCAL -> R.string.search_library
                                    SearchSource.ONLINE -> R.string.search_yt_music
                                }
                            )
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.text.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchQueryChange(TextFieldValue()) }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = null
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                searchSource =
                                    if (searchSource == SearchSource.ONLINE) SearchSource.LOCAL else SearchSource.ONLINE
                            }
                        ) {
                            Icon(
                                imageVector = when (searchSource) {
                                    SearchSource.LOCAL -> Icons.Rounded.LibraryMusic
                                    SearchSource.ONLINE -> Icons.Rounded.Language
                                },
                                contentDescription = null
                            )
                        }
                    },
                    windowInsets = windowInsets,
                    focusRequester = searchBarFocusRequester,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxSize()
                ) {
                    Crossfade(
                        targetState = searchSource,
                        label = "",
                        modifier = Modifier
                            .fillMaxSize()
                    ) { searchSource ->
                        val replaceSearchListState = rememberLazyListState()

                        when (searchSource) {
                            SearchSource.LOCAL -> {
                                val localResults by viewModel.localResult.collectAsState(emptyList())
                                LazyColumn(
                                    state = replaceSearchListState,
                                ) {
                                    items(
                                        items = localResults,
                                        key = { it.id }
                                    ) { item ->
                                        M3uSongSearchListItem(
                                            song = item,
                                            onSearchResultClick = {
                                                val prevSongQuery = viewModel.importedSongs[searchId].query
                                                viewModel.importedSongs[searchId] =
                                                    ImportedSong(
                                                        query = prevSongQuery,
                                                        song = item,
                                                        uuid = UUID.randomUUID().toString(),
                                                        status = ImportM3uFilter.IMPORTED
                                                    )

                                                m3uNavController.navigate(Screens.M3uList.route)
                                                onSearchQueryChange(TextFieldValue())
                                            },
                                        )
                                    }
                                }
                            }


                            SearchSource.ONLINE -> {
                                LaunchedEffect(replaceSearchListState) {
                                    snapshotFlow {
                                        replaceSearchListState.layoutInfo.visibleItemsInfo.any { it.key == "loading" }
                                    }.collect { shouldLoadMore ->
                                        if (!shouldLoadMore) return@collect
                                        viewModel.loadMore()
                                    }
                                }


                                val ytItemContent: @Composable LazyItemScope.(YTItem, List<YTItem>) -> Unit =
                                    { item: YTItem, collection: List<YTItem> ->
                                        fun onClick() {
                                            val prevSongQuery =
                                                viewModel.importedSongs[searchId].query
                                            val songItem = (item as? SongItem)
                                            if (songItem == null) return
                                            val song = Song(
                                                song = songItem.toMediaMetadata().toSongEntity(),
                                                artists = songItem.artists.map {
                                                    ArtistEntity(
                                                        id = it.id
                                                            ?: ArtistEntity.generateArtistId(),
                                                        name = it.name
                                                    )
                                                }
                                            )
                                            viewModel.importedSongs[searchId] =
                                                ImportedSong(
                                                    query = prevSongQuery,
                                                    song = song,
                                                    uuid = UUID.randomUUID().toString(),
                                                    status = ImportM3uFilter.IMPORTED
                                                )

                                            m3uNavController.navigate(Screens.M3uList.route)
                                            onSearchQueryChange(TextFieldValue())
                                        }

                                        val content: @Composable () -> Unit = {
                                            YouTubeListItem(
                                                item = item,
                                                trailingContent = {
                                                    IconButton(
                                                        onClick = { onClick() }
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.SwapHoriz,
                                                            contentDescription = null
                                                        )
                                                    }

                                                },
                                                modifier = Modifier
                                                    .combinedClickable(
                                                        onClick = {
                                                            if (item is SongItem){
                                                                if (item.id == playerConnection?.mediaMetadata?.value?.id) {
                                                                    playerConnection.player.togglePlayPause()
                                                                } else {
                                                                    playerConnection?.playQueue(
                                                                        ListQueue(
                                                                            items = listOf(item.toMediaMetadata()),
                                                                            startIndex = 0,
                                                                        )
                                                                    )
                                                                }
                                                            }
                                                        },
                                                    )
                                            )
                                        }

                                        content()
                                    }
                                val onlineResults by viewModel.onlineResult.collectAsState()
                                LazyColumn(
                                    state = replaceSearchListState,
                                ) {
                                    if (onlineResults == null) {
                                        item {
                                            EmptyPlaceholder(
                                                icon = Icons.Rounded.Search,
                                                text = stringResource(R.string.no_results_found),
                                                modifier = Modifier.animateItem()
                                            )
                                        }
                                    } else {
                                        items(
                                            items = onlineResults!!.items,
                                            key = { it.id }
                                        ) { item ->
                                            ytItemContent(item, onlineResults!!.items)
                                        }

                                        if (onlineResults!!.continuation != null && searchQuery.text != "") {
                                            item(key = "loading") {
                                                ShimmerHost {
                                                    repeat(3) {
                                                        ListItemPlaceHolder()
                                                    }
                                                }
                                            }
                                        }

                                        if (onlineResults!!.items.isEmpty()) {
                                            item {
                                                EmptyPlaceholder(
                                                    icon = Icons.Rounded.Search,
                                                    text = stringResource(R.string.no_results_found),
                                                    modifier = Modifier.animateItem()
                                                )
                                            }
                                        }
                                    }

                                    if (searchQuery.text != "" && (onlineResults == null || onlineResults!!.continuation != null)) {
                                        item {
                                            ShimmerHost {
                                                repeat(8) {
                                                    ListItemPlaceHolder()
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        // jesus christ lord almighty


        LazyColumnScrollbar(
            state = mainListState,
        )

        TopAppBar(
            title = {
                if (isSearching) {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
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
                } else {
                    Text(stringResource(R.string.import_playlist))
                }
            },
            actions = {
                if (!isSearching && navBackStackEntry?.destination?.route?.let { (it == Screens.M3uList.route) } == true) {
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
                    onClick = {
                        handleBack()
                    },
                    onLongClick = {
                        if (!isSearching) {
                            navController.backToMain()
                        }
                    }
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

        FloatingFooter(inSelectMode) {
            SelectHeaderM3u(
                selectedItems = selection,
                totalItemCount = viewModel.importedSongs.size,
                onSelectAll = {
                    selection.addAll(
                        elements = viewModel.importedSongs.filter {
                            when (importedChipsValue) {
                                ImportM3uFilter.IMPORTED -> it.status == ImportM3uFilter.IMPORTED || it.status == ImportM3uFilter.MISMATCH
                                ImportM3uFilter.MISSING -> it.status == ImportM3uFilter.MISSING
                                ImportM3uFilter.MISMATCH -> it.status == ImportM3uFilter.MISMATCH
                                else -> true
                            }
                        }.map { it.uuid }
                        .filter { !selection.contains(it)}
                    )
                },
                onDeselectAll = { selection.clear() },
                menuState = menuState,
                onDismiss = onExitSelectionMode,
                importM3uViewModel = viewModel
            )
        }

        // TODO: in the future, this will likely be its own full page with no navbar
//        SnackbarHost(
//            hostState = snackbarHostState,
//            modifier = Modifier
//                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime))
//                .align(Alignment.BottomCenter)
//        )
    }

    if (showChoosePlaylistDialog) {
        AddToPlaylistDialog(
            navController = navController,
            allowSyncing = false,
            initialTextFieldValue = importedTitle,
            songIds = viewModel.importedSongs.filter {
                it.status == ImportM3uFilter.IMPORTED ||
                it.status == ImportM3uFilter.MISMATCH
            }.map { (_, song, _, _) -> song.id },
            onPreAdd = {
                viewModel.importedSongs.map { (_, song, _, _) -> song }.forEach {
                    database.insert(it.toMediaMetadata())
                }
                emptyList()
            },
            onDismiss = { showChoosePlaylistDialog = false }
        )
    }

    if (showExitConfirm) {
        DefaultDialog(
            onDismiss = { showExitConfirm = false },
            content = {
                Text(
                    text = stringResource(R.string.import_m3u_exit_confirm),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
            },
            buttons = {
                TextButton(
                    onClick = { showExitConfirm = false }
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }

                TextButton(
                    onClick = {
                        navController.navigateUp()
                        showExitConfirm = false
                    }
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            }
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
): Pair<ArrayList<ImportedSong>, String> = withContext(Dispatchers.IO) {
    val unorderedSongs = ArrayList<Pair<Int, ImportedSong>>()

    var songs = ArrayList<ImportedSong>()
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
                                    val suggestionSongs =
                                        suggestions?.recommendedItems.orEmpty().distinctBy { it.id }
                                            .filter { it is SongItem }
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

                                // take first song when searching on YTM
                                if (matchStrength == ScannerM3uMatchCriteria.LEVEL_0 && searchOnline && matches.isNotEmpty()) {
                                    unorderedSongs.add(
                                        Pair(
                                            index,
                                            ImportedSong(
                                                query = query,
                                                song = matches.first(),
                                                uuid = UUID.randomUUID().toString(),
                                                status = if (matches.first().title == title) ImportM3uFilter.IMPORTED else ImportM3uFilter.MISMATCH
                                            )
                                        )
                                    )
                                } else {
                                    for (s in matches) {
                                        if (compareM3uSong(
                                                mockSong,
                                                s,
                                                matchStrength = matchStrength
                                            )
                                        ) {
                                            unorderedSongs.add(
                                                Pair(
                                                    index,
                                                    ImportedSong(
                                                        query = query,
                                                        song = s,
                                                        uuid = UUID.randomUUID().toString(),
                                                        status = if (s.title == title) ImportM3uFilter.IMPORTED else ImportM3uFilter.MISMATCH
                                                    )
                                                )
                                            )
                                            break
                                        }
                                    }
                                }

                                if (oldSize == unorderedSongs.size) {
                                    unorderedSongs.add(
                                        Pair(
                                            index,
                                            ImportedSong(
                                                query = query,
                                                song = mockSong,
                                                uuid = UUID.randomUUID().toString(),
                                                status = ImportM3uFilter.MISSING
                                            )
                                        )
                                    )
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
                songs = unorderedSongs.map { (_, importedSong) ->
                    importedSong
                } as ArrayList<ImportedSong>
            }
        }
    }.onFailure {
        if (it !is CancellationException) {
            reportException(it)
            Toast.makeText(context, R.string.m3u_import_playlist_failed, Toast.LENGTH_SHORT).show()
        }
    }

    if (songs.isEmpty()) {
        CoroutineScope(Dispatchers.IO).launch {
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
    Pair(songs, fileName)
}


/**
 * Read a file to a string
 */
fun InputStream.readLines(): List<String> {
    return this.bufferedReader().useLines { it.toList() }
}

fun queryMatchesSong(query: String, song: Song): Boolean {
    val songData = "${song.title} ${Uri.decode(song.artists.joinToString(" "))}"
    return songData.lowercase().contains(query.lowercase())
}

enum class ImportM3uFilter {
    ALL, IMPORTED, MISSING, MISMATCH
}
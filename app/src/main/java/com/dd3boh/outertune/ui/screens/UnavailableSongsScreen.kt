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
import android.util.Log
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
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
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
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
import com.dd3boh.outertune.R
import com.dd3boh.outertune.constants.AlbumThumbnailSize
import com.dd3boh.outertune.constants.SearchSource
import com.dd3boh.outertune.constants.SearchSourceKey
import com.dd3boh.outertune.constants.SongSortDescendingKey
import com.dd3boh.outertune.constants.SongSortType
import com.dd3boh.outertune.constants.SongSortTypeKey
import com.dd3boh.outertune.constants.ThumbnailCornerRadius
import com.dd3boh.outertune.constants.TopBarInsets
import com.dd3boh.outertune.db.DatabaseDao
import com.dd3boh.outertune.db.entities.ArtistEntity
import com.dd3boh.outertune.db.entities.Song
import com.dd3boh.outertune.db.entities.SongEntity
import com.dd3boh.outertune.extensions.toEnum
import com.dd3boh.outertune.extensions.togglePlayPause
import com.dd3boh.outertune.models.toMediaMetadata
import com.dd3boh.outertune.playback.queues.ListQueue
import com.dd3boh.outertune.ui.component.EmptyPlaceholder
import com.dd3boh.outertune.ui.component.LazyColumnScrollbar
import com.dd3boh.outertune.ui.component.SearchBar
import com.dd3boh.outertune.ui.component.button.IconButton
import com.dd3boh.outertune.ui.component.items.UnavailableSongListItem
import com.dd3boh.outertune.ui.component.items.UnavailableSongSearchListItem
import com.dd3boh.outertune.ui.component.items.YouTubeListItem
import com.dd3boh.outertune.ui.component.shimmer.ListItemPlaceHolder
import com.dd3boh.outertune.ui.component.shimmer.ShimmerHost
import com.dd3boh.outertune.ui.dialog.DefaultDialog
import com.dd3boh.outertune.ui.menu.UnavailableSongMenu
import com.dd3boh.outertune.ui.utils.backToMain
import com.dd3boh.outertune.utils.YTPlayerUtils
import com.dd3boh.outertune.utils.dataStore
import com.dd3boh.outertune.utils.rememberEnumPreference
import com.dd3boh.outertune.utils.scanners.LocalMediaScanner
import com.dd3boh.outertune.viewmodels.UnavailableSong
import com.dd3boh.outertune.viewmodels.UnavailableSongsViewModel
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.YTItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.collections.distinctBy
import kotlin.collections.map
import kotlin.collections.orEmpty

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun UnavailableSongsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: UnavailableSongsViewModel = hiltViewModel()
) {

    val context = LocalContext.current
    val database = LocalDatabase.current
    val focusRequester = remember { FocusRequester() }
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current
    val windowInsets = LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime)

    var searchSource by rememberEnumPreference(SearchSourceKey, SearchSource.ONLINE)

    var isLoading by rememberSaveable { mutableStateOf(false) }

    var showExitConfirm by rememberSaveable {
        mutableStateOf(false)
    }
    var toSwapIndex by rememberSaveable { mutableIntStateOf(0) }

    val mainListState = rememberSaveable(saver = LazyListState.Saver) {
        LazyListState()
    }


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

    val navigationItems = listOf(Screens.UnavailableSongsList, Screens.UnavailableSongsSearch)
    val unavailableSongsNavController = rememberNavController()
    val navBackStackEntry by unavailableSongsNavController.currentBackStackEntryAsState()

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

    val importJob = remember { SupervisorJob() }

    DisposableEffect(Unit) {
        onDispose {
            importJob.cancel()
        }
    }

    LaunchedEffect(Unit) {
        isLoading = true
        viewModel.unavailableSongs.clear()

        viewModel.unavailableSongs.addAll(scanForUnavailableSongs(
            database,
            context,
            onPercentageChange = {
                percentage = it
            }
        ))

        isLoading = false
    }

    fun handleBack() {
        if (navBackStackEntry?.destination?.route?.let { (it == Screens.UnavailableSongsSearch.route) } == true) {
            onSearchQueryChange(TextFieldValue())
            unavailableSongsNavController.navigate(Screens.UnavailableSongsList.route)
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
            navController = unavailableSongsNavController,
            startDestination = Screens.UnavailableSongsList.route,
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
            composable(Screens.UnavailableSongsList.route) {
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

                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Button(
                                    onClick =  {
                                        replaceAllWithAutoSearch(
                                            viewModel = viewModel,
                                            database = database,
                                            onLoadingChange = {
                                                isLoading = it
                                            })
                                    },
                                    enabled = viewModel.unavailableSongs.isNotEmpty()
                                ) {
                                    Text(stringResource(R.string.replace_all_with_autosearch))
                                }
                            }
                        }
                    }
                    if (isLoading) {
                        item {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = if (viewModel.unavailableSongs.isEmpty()) 210.dp else 20.dp)
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
                    } else if (viewModel.unavailableSongs.isEmpty()) {
                        item {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = 150.dp)
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
                                    text = stringResource(R.string.no_unavailable_songs_found),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    if (viewModel.unavailableSongs.isNotEmpty()) {
                        val songs = viewModel.unavailableSongs
                        itemsIndexed(
                            items = songs,
                            key = { _, (_, uuid) -> uuid }
                        ) { index, (song, uuid) ->
                            UnavailableSongListItem (
                                song = song,
                                isMissing = false,
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
                                        UnavailableSongMenu (
                                            song = song,
                                            modelIndex = Pair(viewModel, uuid),
                                            navController = navController,
                                            unavailableSongsNavController = unavailableSongsNavController,
                                            onSwapClick = {
                                                val queryText = "${song.song.title} ${Uri.decode(song.artists.joinToString(" ") {it.name})}"
                                                onSearchQueryChange(TextFieldValue(queryText))
                                            },
                                            onDismiss = menuState::dismiss
                                        )
                                    }
                                    toSwapIndex = index
                                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                                    onSearchQueryChange(
                                        TextFieldValue(
                                            text = query.text,
                                            selection = query.selection
                                        )
                                    )
                                },
                                modifier = Modifier
                                    .background (
                                        MaterialTheme.colorScheme.background
                                    )
                                    .combinedClickable(
                                        onClick = {
                                            if (inSelectMode) {
                                                if (selection.contains(uuid)) {
                                                    selection.remove(uuid)
                                                } else {
                                                    selection.add(uuid)
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
            composable(Screens.UnavailableSongsSearch.route) {
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
                                        UnavailableSongSearchListItem(
                                            song = item,
                                            onSearchResultClick = {
                                                val old = viewModel.unavailableSongs[toSwapIndex]
                                                viewModel.unavailableSongs[toSwapIndex] =
                                                    UnavailableSong(
                                                        song = item,
                                                        uuid = old.uuid,
                                                    )

                                                CoroutineScope(Dispatchers.IO).launch {
                                                    database.delete(item.song)
                                                    database.changeSongId(old.song.id, item.id)
                                                    val newSong = Song(
                                                        song = SongEntity(
                                                            id = item.id,
                                                            title = item.title,
                                                            duration = item.song.duration,
                                                            thumbnailUrl = item.song.thumbnailUrl,
                                                            trackNumber = item.song.trackNumber,
                                                            discNumber = item.song.discNumber,
                                                            albumId = item.song.albumId,
                                                            albumName = item.song.albumName,
                                                            year = old.song.song.year,
                                                            date = old.song.song.date,
                                                            dateModified = old.song.song.dateModified,
                                                            isLocal = true,
                                                            inLibrary = old.song.song.inLibrary,
                                                            localPath = item.song.localPath
                                                        ),
                                                        artists = item.artists,
                                                        album = item.album,
                                                        genre = item.genre
                                                    )
                                                    database.update(newSong.song)
                                                }
                                                unavailableSongsNavController.navigate(Screens.UnavailableSongsList.route)
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
                                    { item: YTItem, _: List<YTItem> ->
                                        fun onClick() {
                                            val songItem = (item as? SongItem) ?: return
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
                                            val old = viewModel.unavailableSongs[toSwapIndex]

                                            CoroutineScope(Dispatchers.IO).launch {
                                                database.delete(song.song)
                                                database.changeSongId(old.song.id, song.id)
                                                val newSong = Song(
                                                    song = SongEntity(
                                                            id = song.id,
                                                            title = song.song.title,
                                                            duration = song.song.duration,
                                                            thumbnailUrl = song.song.thumbnailUrl,
                                                            inLibrary = old.song.song.inLibrary,
                                                            isLocal = song.song.isLocal,
                                                            localPath = song.song.localPath,
                                                            dateDownload = old.song.song.dateDownload,
                                                            liked = old.song.song.liked,
                                                            likedDate = old.song.song.likedDate,
                                                            trackNumber = song.song.trackNumber,
                                                            discNumber = song.song.discNumber,
                                                            albumId = song.song.albumId,
                                                            albumName = song.song.albumName,
                                                            year = old.song.song.year,
                                                            date = old.song.song.date,
                                                            dateModified = old.song.song.dateModified,
                                                    ),
                                                    artists = song.artists,
                                                    album = song.album,
                                                    genre = song.genre
                                                )
                                                viewModel.unavailableSongs[toSwapIndex] =
                                                    UnavailableSong(
                                                        song = newSong,
                                                        uuid = old.uuid,
                                                    )
                                                database.update(newSong.song)
                                            }

                                            unavailableSongsNavController.navigate(Screens.UnavailableSongsList.route)
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
                    Text(stringResource(R.string.local_player_settings_scan_for_unavailable_songs))
                }
            },
            actions = {
                if (!isSearching && navBackStackEntry?.destination?.route?.let { (it == Screens.UnavailableSongsList.route) } == true) {
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
    }

    if (showExitConfirm) {
        DefaultDialog(
            onDismiss = { showExitConfirm = false },
            content = {
                Text(
                    text = stringResource(R.string.exit_confirm),
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

fun replaceAllWithAutoSearch(
    viewModel: UnavailableSongsViewModel,
    database: DatabaseDao,
    onLoadingChange: (Boolean) -> Unit
) {
    CoroutineScope(Dispatchers.IO).launch {
        onLoadingChange(true)
        viewModel.unavailableSongs.forEachIndexed { index, old ->
            val matches = mutableListOf<Song>()
            val queryText = "${old.song.title} ${Uri.decode(old.song.artists.joinToString(" ") {it.name})}"
            val suggestions = YouTube.searchSuggestions(queryText).getOrNull()
            val suggestionSongs =
                suggestions?.recommendedItems.orEmpty().distinctBy { it.id }
                    .filterIsInstance<SongItem>()
            suggestionSongs.forEach { suggestion ->
                val song = (suggestion).toMediaMetadata()
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
                LocalMediaScanner.youtubeSongLookup(queryText, songUrl = null)
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

            matches.distinctBy { it.id }

            val song = matches[0].copy()
            CoroutineScope(Dispatchers.IO).launch {
                database.delete(song.song)
                database.changeSongId(old.song.id, song.id)
                val newSong = Song(
                    song = SongEntity(
                        id = song.id,
                        title = song.song.title,
                        duration = song.song.duration,
                        thumbnailUrl = song.song.thumbnailUrl,
                        inLibrary = old.song.song.inLibrary,
                        isLocal = song.song.isLocal,
                        localPath = song.song.localPath,
                        dateDownload = old.song.song.dateDownload,
                        liked = old.song.song.liked,
                        likedDate = old.song.song.likedDate,
                        trackNumber = song.song.trackNumber,
                        discNumber = song.song.discNumber,
                        albumId = song.song.albumId,
                        albumName = song.song.albumName,
                        year = old.song.song.year,
                        date = old.song.song.date,
                        dateModified = old.song.song.dateModified,
                    ),
                    artists = song.artists,
                    album = song.album,
                    genre = song.genre
                )
                viewModel.unavailableSongs[index] =
                    UnavailableSong(
                        song = newSong,
                        uuid = old.uuid,
                    )
                database.update(newSong.song)
            }
        }
        onLoadingChange(false)
    }
}


@OptIn(ExperimentalCoroutinesApi::class)
suspend fun scanForUnavailableSongs(
    database: DatabaseDao,
    context: Context,
    onPercentageChange: (Int) -> Unit
): MutableList<UnavailableSong> {
    val unavailableSongs = mutableStateListOf<UnavailableSong>()
    var processedCount = 0
    val songs = context.dataStore.data
        .map {
            it[SongSortTypeKey].toEnum(SongSortType.CREATE_DATE) to (it[SongSortDescendingKey] ?: true)
        }
        .distinctUntilChanged()
        .flatMapLatest { (sortType, descending) ->
            database.songs(sortType, descending)
        }
    val songsArray = songs.first()
    songsArray.forEachIndexed { index, song ->
        if (YTPlayerUtils.playerResponseForMetadata(song.id).getOrNull()?.playabilityStatus?.status == "UNPLAYABLE") {
            unavailableSongs.add(UnavailableSong(song, UUID.randomUUID().toString()))
            Log.d("PLAYABILITY: ($index)", "ERROR")
        } else {
            Log.d("PLAYABILITY: ($index)", "OK")
        }
        onPercentageChange(processedCount*100/songsArray.size)
        processedCount += 1
    }
    return unavailableSongs
}

// TODO: revert MusicDatabase at the end.
// change UnavailableSOngsScreen to accumulate all songs and process them on open.
// edit UnavailableSongsViewModel accordingly.
/*
 * Copyright (C) 2025 O​u​t​er​Tu​ne Project
 *
 * SPDX-License-Identifier: GPL-3.0
 *
 * For any other attributions, refer to the git commit history
 */
package com.dd3boh.outertune.ui.screens.unavailablesongs

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.dd3boh.outertune.LocalDatabase
import com.dd3boh.outertune.R
import com.dd3boh.outertune.constants.SongSortDescendingKey
import com.dd3boh.outertune.constants.SongSortType
import com.dd3boh.outertune.constants.SongSortTypeKey
import com.dd3boh.outertune.constants.TopBarInsets
import com.dd3boh.outertune.db.DatabaseDao
import com.dd3boh.outertune.db.entities.ArtistEntity
import com.dd3boh.outertune.db.entities.Song
import com.dd3boh.outertune.db.entities.SongEntity
import com.dd3boh.outertune.extensions.toEnum
import com.dd3boh.outertune.models.toMediaMetadata
import com.dd3boh.outertune.ui.component.button.IconButton
import com.dd3boh.outertune.ui.dialog.DefaultDialog
import com.dd3boh.outertune.ui.utils.backToMain
import com.dd3boh.outertune.utils.YTPlayerUtils
import com.dd3boh.outertune.utils.dataStore
import com.dd3boh.outertune.utils.scanners.LocalMediaScanner
import com.dd3boh.outertune.viewmodels.UnavailableSong
import com.dd3boh.outertune.viewmodels.UnavailableSongsViewModel
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
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
    viewModel: UnavailableSongsViewModel
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val focusRequester = remember { FocusRequester() }

    var showExitConfirm by rememberSaveable {
        mutableStateOf(false)
    }

    // search
    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }

    val selection = rememberSaveable(
        saver = listSaver<MutableList<String>, String>(
            save = { it.toList() },
            restore = { it.toMutableStateList() }
        )
    ) { mutableStateListOf() }

    val onExitSelectionMode = {
        viewModel.inSelectMode.value = false
        selection.clear()
    }

    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
        }
    }

    val importJob = remember { SupervisorJob() }

    DisposableEffect(Unit) {
        onDispose {
            importJob.cancel()
        }
    }

    LaunchedEffect(Unit) {
        if (viewModel.unavailableSongs.isEmpty()) {
            viewModel.isLoading.value = true
            viewModel.unavailableSongs.addAll(scanForUnavailableSongs(
                database,
                context,
                onPercentageChange = {
                    viewModel.percentage.intValue = it
                }
            ))
            viewModel.isLoading.value = false
        }
    }

    UnavailableSongsList(
        navController = navController,
        selection = selection,
        viewModel = viewModel
    )
    fun handleBack() {
        if (isSearching) {
            isSearching = false
            query = TextFieldValue()
        } else if (viewModel.inSelectMode.value) {
            onExitSelectionMode()
        } else {
            showExitConfirm = true
        }
    }
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
    context: Context,
    onLoadingChange: (Boolean) -> Unit,
    onPercentageChange: (Int) -> Unit
) {
    CoroutineScope(Dispatchers.Main).launch {
        onPercentageChange(0)
        onLoadingChange(true)
        var count = 0
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

            val onlineResult = LocalMediaScanner.youtubeSongLookup(queryText, songUrl = null)
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
                        query = old.query,
                        song = newSong,
                        uuid = old.uuid,
                    )
                database.update(newSong.song)
            }
            count+=1
            onPercentageChange(count*100/viewModel.unavailableSongs.size)
        }
        onLoadingChange(false)
        Toast.makeText(context, R.string.replaced_all_with_autosearch, Toast.LENGTH_SHORT).show()
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
            val query = "${song.title} ${Uri.decode(song.artists.joinToString(" ") {it.name})}"
            unavailableSongs.add(UnavailableSong(query, song, UUID.randomUUID().toString()))
            Log.d("PLAYABILITY: ($index)", "ERROR")
        } else {
            Log.d("PLAYABILITY: ($index)", "OK")
        }
        onPercentageChange(processedCount*100/songsArray.size)
        processedCount += 1
    }
    return unavailableSongs
}



// TODO: make search work in UnavailableSongsScreen(List)
//

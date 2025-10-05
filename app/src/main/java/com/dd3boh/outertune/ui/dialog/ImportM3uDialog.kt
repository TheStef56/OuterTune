/*
 * Copyright (C) 2025 O​u​t​er​Tu​ne Project
 *
 * SPDX-License-Identifier: GPL-3.0
 *
 * For any other attributions, refer to the git commit history
 */
package com.dd3boh.outertune.ui.dialog


import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Input
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dd3boh.outertune.LocalDatabase
import com.dd3boh.outertune.LocalSnackbarHostState
import com.dd3boh.outertune.R
import com.dd3boh.outertune.constants.ScannerM3uMatchCriteria
import com.dd3boh.outertune.db.MusicDatabase
import com.dd3boh.outertune.db.entities.ArtistEntity
import com.dd3boh.outertune.db.entities.Song
import com.dd3boh.outertune.db.entities.SongEntity
import com.dd3boh.outertune.models.toMediaMetadata
import com.dd3boh.outertune.ui.component.EnumListPreference
import com.dd3boh.outertune.ui.component.LazyColumnScrollbar
import com.dd3boh.outertune.utils.lmScannerCoroutine
import com.dd3boh.outertune.utils.reportException
import com.dd3boh.outertune.utils.scanners.LocalMediaScanner
import com.dd3boh.outertune.utils.scanners.LocalMediaScanner.Companion.compareM3uSong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.InputStream

fun clampText(string: String, maxLength: Int): String {
    return if (string.length > maxLength) {
        string.take(maxLength) + "..."
    } else {
        string
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropDownResults(
    title: String,
    items: List<String>,
    state: LazyListState,
    songs: List<Pair<String, Song>>,
    searchId: MutableState<Int?>?
) {
    var expanded by remember { mutableStateOf(false) }


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null
            )
        }

        // Expandable content
        if (expanded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    state = state,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 150.dp)
                ) {
                    itemsIndexed(
                        items = items,
                        key = { _, song -> song.hashCode() }
                    ) { index, item ->
                        Row (
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = clampText("${index + 1}: $item", 22),
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .fillMaxWidth(fraction = .85f)
                                    .padding(vertical = 4.dp)
                            )
                            if (songs.isNotEmpty()){
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .clickable {
                                            searchId?.value = index
                                        }
                                )
                            }
                        }
                    }
                }
                LazyColumnScrollbar(
                    state = state,
                    modifier = Modifier
                        .heightIn(max = 150.dp)
                )
            }
        }
    }
}

@Composable
fun ImportM3uDialog(
    navController: NavController,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val snackbarHostState = LocalSnackbarHostState.current

    var scannerSensitivity by remember {
        mutableStateOf(ScannerM3uMatchCriteria.LEVEL_1)
    }

    var remoteLookup by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }
    var importedTitle by remember { mutableStateOf("") }
    val importedSongs = rememberSaveable { mutableStateListOf<Pair<String, Song>>() }
    val rejectedSongs = rememberSaveable { mutableStateListOf<String>() }
    val searchId = remember { mutableStateOf<Int?>(null) }

    val importM3uLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        CoroutineScope(lmScannerCoroutine).launch {
            try {
                isLoading = true
                if (uri != null) {
                    val result = loadM3u(
                        context = context,
                        database = database,
                        snackbarHostState = snackbarHostState,
                        uri = uri,
                        matchStrength = scannerSensitivity,
                        searchOnline = remoteLookup
                    )
                    importedSongs.clear()
                    importedSongs.addAll(result.first)
                    rejectedSongs.clear()
                    rejectedSongs.addAll(result.second)
                    importedTitle = result.third
                }
            } catch (e: Exception) {
                reportException(e)
            } finally {
                isLoading = false
            }
        }

    }


    DefaultDialog(
        onDismiss = onDismiss,
        icon = { Icon(Icons.AutoMirrored.Rounded.Input, null) },
        title = { Text(stringResource(R.string.import_playlist)) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // Preferences section
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = remoteLookup,
                    onCheckedChange = { remoteLookup = it }
                )
                Text(
                    text = stringResource(R.string.m3u_ytm_lookup),
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 14.sp
                )
            }

            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                }

                Button(
                    onClick = {
                        importedSongs.clear()
                        rejectedSongs.clear()
                        importM3uLauncher.launch(arrayOf("audio/*"))
                    },
                    enabled = !isLoading
                ) {
                    Text(stringResource(R.string.m3u_import_playlist))
                }
            }

            if (importedSongs.isNotEmpty()) {
                val importedListState = rememberLazyListState()

                DropDownResults(
                    title = stringResource(R.string.import_success_songs),
                    items = importedSongs.map { (_, song) -> song.title },
                    state = importedListState,
                    songs = importedSongs,
                    searchId = searchId
                )
            }

            if (rejectedSongs.isNotEmpty()) {
                val rejectedListState = rememberLazyListState()

                DropDownResults(
                    title = stringResource(R.string.import_failed_songs),
                    items = rejectedSongs,
                    state = rejectedListState,
                    songs = emptyList<Pair<String, Song>>(),
                    searchId = null
                )
            }
            if (searchId.value != null) {
                val route = "search_sub/${Uri.encode(importedSongs.map {(query, _ ) -> query}[searchId.value!!])}"
                navController.navigate(route)
                searchId.value = null
            }
        }
        // Bottom buttons

        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }

            TextButton(
                onClick = { showChoosePlaylistDialog = true },
                enabled = importedSongs.isNotEmpty()
            ) {
                Text(stringResource(R.string.add_to_playlist))
            }
        }
    }


    /**
     * ---------------------------
     * Dialogs
     * ---------------------------
     */

    if (showChoosePlaylistDialog) {
        AddToPlaylistDialog(
            navController = navController,
            allowSyncing = false,
            initialTextFieldValue = importedTitle,
            songIds = importedSongs.map { (_, song) -> song.id },
            onPreAdd = {
                importedSongs.map{(_, song) -> song}.forEach {
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
    searchOnline: Boolean = false
): Triple<ArrayList<Pair<String, Song>>, ArrayList<String>, String> {
    val songs = ArrayList<Pair<String, Song>>()
    val rejectedSongs = ArrayList<String>()

    runCatching {
        context.applicationContext.contentResolver.openInputStream(uri)?.use { stream ->
            val lines = stream.readLines()
            if (lines.isEmpty()) return@runCatching
            if (lines.first().startsWith("#EXTM3U")) {
                lines.forEachIndexed { index, rawLine ->
                    if (rawLine.startsWith("#EXTINF:")) {
                        // maybe later write this to be more efficient
                        val artists =
                            rawLine.substringAfter("#EXTINF:").substringAfter(',').substringBefore(" - ").split(';')
                        val title = rawLine.substringAfter("#EXTINF:").substringAfter(',').substringAfter(" - ")
                        val source = if (index + 1 < lines.size) lines[index + 1] else null

                        val mockSong = Song(
                            song = SongEntity(
                                id = "",
                                title = title,
                                isLocal = true,
                                localPath = if (source?.startsWith("http") == false) source.substringAfter(',') else null
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
                                id = source.substringAfter("watch?").substringAfter("=").substringBefore('?')
                            }
                            val dbResult = mutableListOf(database.song(id).first())
                            dbResult.addAll(database.searchSongsInDb(title).first())
                            dbResult.filterNotNull().toMutableList()
                        }
                        // do not search for local songs
                        val query = "$title ${Uri.decode(artists.joinToString(" "))}"
                        if (searchOnline && matches.isEmpty() && source?.contains(',') == false) {
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
                        val oldSize = songs.size
                        var foundOne = false // TODO: Eventually the user can pick from matches... eventually...

                        // take first song when searching on YTM
                        if (matchStrength == ScannerM3uMatchCriteria.LEVEL_0 && searchOnline && matches.isNotEmpty()) {
                            songs.add(Pair(query, matches.first()))
                        } else {
                            for (s in matches) {
                                if (compareM3uSong(mockSong, s, matchStrength = matchStrength)) {
                                    songs.add(Pair("", s))
                                    foundOne = true
                                    break
                                }
                            }
                        }

                        if (oldSize == songs.size) {
                            rejectedSongs.add(rawLine)
                        }
                    }
                }
            }
        }
    }.onFailure {
        reportException(it)
        Toast.makeText(context, R.string.m3u_import_playlist_failed, Toast.LENGTH_SHORT).show()
    }

    if (songs.isEmpty()) {
        CoroutineScope(Dispatchers.Main).launch {
            snackbarHostState.showSnackbar(
                message = context.getString(R.string.m3u_import_failed),
                withDismissAction = true,
                duration = SnackbarDuration.Long
            )
        }
    }
    return Triple(songs, rejectedSongs, uri.path?.substringAfterLast('/')?.substringBeforeLast('.') ?: "")
}

/**
 * Read a file to a string
 */
fun InputStream.readLines(): List<String> {
    return this.bufferedReader().useLines { it.toList() }
}

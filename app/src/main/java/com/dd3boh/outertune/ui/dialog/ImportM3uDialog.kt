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
import android.provider.OpenableColumns
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Input
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
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
import androidx.compose.runtime.mutableIntStateOf
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
import com.zionhuang.innertube.YouTube
import com.zionhuang.innertube.models.SongItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.InputStream
import kotlin.collections.distinctBy
import kotlin.collections.orEmpty

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropDownResults(
    title: String,
    items: List<String>,
    state: LazyListState,
    songs: MutableList<Pair<String, Song>>,
    searchId: MutableState<Pair<Boolean, Int>?>?,
    expanded: MutableState<Boolean>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded.value = !expanded.value }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded.value) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null
            )
        }

        // Expandable content
        if (expanded.value) {
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
                        key = { index, _ -> index }
                    ) { index, item ->
                        Row (
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = "${index + 1}: $item",
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .fillMaxWidth(fraction = .70f)
                                    .padding(vertical = 4.dp)
                            )
                            if (songs.isNotEmpty()){
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .clickable {
                                            songs.removeAt(index)
                                        }
                                )
                                Spacer(Modifier.width(5.dp))
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .clickable {
                                            searchId!!.value = Pair(true, index)
                                        }
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.width(70.dp))
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
    replaceSong: MutableState<Song?>,
    onDismiss: () -> Unit,
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
    val importedSongs = rememberSaveable { mutableStateListOf<Pair<String, Song>>() }
    val rejectedSongs = rememberSaveable { mutableStateListOf<String>() }
    val searchId = rememberSaveable { mutableStateOf <Pair<Boolean, Int>?> (Pair(false, 0)) }

    val importedListState = rememberSaveable ( saver = LazyListState.Saver ) {
        LazyListState()
    }

    val rejectedListState = rememberSaveable ( saver = LazyListState.Saver ) {
        LazyListState()
    }

    val importedExpanded = rememberSaveable { mutableStateOf(false) }
    val rejectedExpanded = rememberSaveable { mutableStateOf(false) }

    var percentage by remember { mutableIntStateOf(0) }

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
                        searchOnline = remoteLookup,
                        onPercentageChange = { newVal ->
                            percentage = newVal
                        }
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
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                        Text(
                            text = "$percentage%",
                            fontSize = 14.sp,
                        )
                        CircularProgressIndicator(
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(40.dp)
                        )
                    }
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
                DropDownResults(
                    title = "${stringResource(R.string.import_success_songs)} (${importedSongs.size})",
                    items = importedSongs.map { (_, song) -> song.title },
                    state = importedListState,
                    songs = importedSongs,
                    searchId = searchId,
                    expanded = importedExpanded
                )
            }

            if (rejectedSongs.isNotEmpty()) {
                val emptyMutableList: MutableList<Pair<String, Song>> = mutableListOf()
                DropDownResults(
                    title = "${stringResource(R.string.import_failed_songs)} (${rejectedSongs.size})",
                    items = rejectedSongs,
                    state = rejectedListState,
                    songs = emptyMutableList,
                    searchId = null,
                    expanded = rejectedExpanded
                )
            }
            if (searchId.value != null && searchId.value!!.first) {
                val route = "search_rep/${Uri.encode(importedSongs.map {(query, _ ) -> query}[searchId.value!!.second])}?rep=true"
                navController.navigate(route)
                searchId.value = Pair(false, searchId.value!!.second)
            }
            if (replaceSong.value != null && searchId.value != null) {
                val prevSongQuery = importedSongs[searchId.value!!.second].first
                importedSongs[searchId.value!!.second] = Pair(prevSongQuery, replaceSong.value) as Pair<String, Song>
                replaceSong.value = null
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
    searchOnline: Boolean = false,
    onPercentageChange: (Int) -> Unit
): Triple<ArrayList<Pair<String, Song>>, ArrayList<String>, String> {
    val unorderedSongs = ArrayList<Triple<Int, String, Song>>()
    val unorderedRejectedSongs = ArrayList<Pair<Int, String>>()

    val scope = CoroutineScope(Dispatchers.Main)

    var songs = ArrayList<Pair<String, Song>>()
    var rejectedSongs = ArrayList<String>()
    var toProcess = 0
    var processed = 0

    runCatching {
        context.applicationContext.contentResolver.openInputStream(uri)?.use { stream ->
            val lines = stream.readLines()
            if (lines.isEmpty()) return@runCatching
            if (lines.first().startsWith("#EXTM3U")) {
                toProcess = lines.size/2
                lines.forEachIndexed { index, rawLine ->
                    scope.launch {
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
                                val suggestionSongs = suggestions?.recommendedItems.orEmpty().distinctBy { it.id }.filter{it is SongItem}
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
                                unorderedSongs.add(Triple(index,query, matches.first()))
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
                                unorderedRejectedSongs.add(Pair(index, rawLine))
                            }
                            processed += 1
                            val percent =
                                if (toProcess > 0) ((processed.toFloat() / toProcess) * 100).toInt() else 0
                            onPercentageChange(percent)
                        }
                    }
                }
                while (processed < toProcess) {
                    delay(10)
                }
                unorderedSongs.sortBy { it.first }
                unorderedRejectedSongs.sortBy { it.first }
                songs = unorderedSongs.map { (_, query, song) -> Pair(query, song)} as ArrayList<Pair<String, Song>>
                rejectedSongs = unorderedRejectedSongs.map { (_, rawLine) -> rawLine} as ArrayList<String>
                onPercentageChange(0)
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
    return Triple(songs, rejectedSongs, fileName)
}

/**
 * Read a file to a string
 */
fun InputStream.readLines(): List<String> {
    return this.bufferedReader().useLines { it.toList() }
}

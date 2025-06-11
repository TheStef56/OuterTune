/*
 * Copyright (C) 2025 O‌ute‌rTu‌ne Project
 *
 * SPDX-License-Identifier: GPL-3.0
 *
 * For any other attributions, refer to the git commit history
 */

package com.dd3boh.outertune.utils.scanners

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.datastore.preferences.core.edit
import androidx.documentfile.provider.DocumentFile
import com.dd3boh.outertune.constants.AutomaticScannerKey
import com.dd3boh.outertune.constants.ENABLE_FFMETADATAEX
import com.dd3boh.outertune.constants.SCANNER_DEBUG
import com.dd3boh.outertune.constants.SYNC_SCANNER
import com.dd3boh.outertune.constants.ScannerImpl
import com.dd3boh.outertune.constants.ScannerImplKey
import com.dd3boh.outertune.constants.ScannerMatchCriteria
import com.dd3boh.outertune.db.MusicDatabase
import com.dd3boh.outertune.db.entities.Artist
import com.dd3boh.outertune.db.entities.ArtistEntity
import com.dd3boh.outertune.db.entities.Song
import com.dd3boh.outertune.db.entities.SongArtistMap
import com.dd3boh.outertune.db.entities.SongEntity
import com.dd3boh.outertune.db.entities.SongGenreMap
import com.dd3boh.outertune.models.CulmSongs
import com.dd3boh.outertune.models.DirectoryTree
import com.dd3boh.outertune.models.MediaMetadata
import com.dd3boh.outertune.models.SongTempData
import com.dd3boh.outertune.models.toMediaMetadata
import com.dd3boh.outertune.ui.utils.scannerSession
import com.dd3boh.outertune.utils.closestMatch
import com.dd3boh.outertune.utils.dataStore
import com.dd3boh.outertune.utils.reportException
import com.zionhuang.innertube.YouTube
import com.zionhuang.innertube.models.ArtistItem
import com.zionhuang.innertube.models.SongItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.time.LocalDateTime
import java.util.Locale
import kotlin.collections.firstOrNull

class LocalMediaScanner(val context: Context, val scannerImpl: ScannerImpl) {
    private val TAG = LocalMediaScanner::class.simpleName.toString()
    private var advancedScannerImpl: MetadataScanner = when (scannerImpl) {
        ScannerImpl.TAGLIB -> TagLibScanner()
        ScannerImpl.FFMPEG_EXT -> if (ENABLE_FFMETADATAEX) FFMpegScanner() else TagLibScanner()
    }

    init {
        Log.i(
            TAG,
            "Creating scanner instance with scannerImpl:  ${advancedScannerImpl.javaClass.name}, requested: $scannerImpl"
        )
    }

    /**
     * Compiles a song with all it's necessary metadata. Unlike MediaStore,
     * this also supports multiple artists, multiple genres (TBD), and a few extra details (TBD).
     */
    fun advancedScan(
        uri: Uri,
    ): SongTempData {
        val file = fileFromUri(context, uri)?: throw IOException("Could not access file")
        try {
            // test if system can play
            val testPlayer = MediaPlayer()
            testPlayer.setDataSource(file.absolutePath)
            testPlayer.prepare()
            testPlayer.release()

            // decide which scanner to use
            val ffmpegData =
                if (advancedScannerImpl is TagLibScanner || (ENABLE_FFMETADATAEX && advancedScannerImpl is FFMpegScanner)) {
                    advancedScannerImpl.getAllMetadataFromFile(file)
                } else {
                    throw RuntimeException("Unsupported extractor")
                }

            return ffmpegData
        } catch (e: Exception) {
            when (e) {
                is IOException, is IllegalArgumentException, is IllegalStateException -> {
                    if (SCANNER_DEBUG) {
                        e.printStackTrace()
                    }
                    throw InvalidAudioFileException("Failed to access file or not in a playable format: ${e.message} for: $uri")
                }

                else -> {
                    if (SCANNER_DEBUG) {
                        Log.w(TAG, "ERROR READING METADATA: ${e.message} for: $uri")
                        e.printStackTrace()
                    }

                    // we still want the song to be playable even if metadata extractor fails
                    return SongTempData(
                        Song(
                            SongEntity(
                                SongEntity.generateSongId(),
                                file.absolutePath.substringAfterLast('/'),
                                thumbnailUrl = null,
                                isLocal = true,
                                inLibrary = LocalDateTime.now(),
                                localPath = file.absolutePath
                            ),
                            artists = ArrayList()
                        ),
                        null
                    )
                }
            }
        }

    }


    /**
     * Scan MediaStore for songs given a list of paths to scan for.
     * This will replace all data in the database for a given song.
     *
     * @param scanPaths List of whitelist paths to scan under. This assumes
     * the current directory is /storage/emulated/0/ a.k.a, /sdcard.
     * For example, to scan under Music and Documents/songs --> ("Music", Documents/songs)
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun scanLocal(
        scanPaths: String,
        excludedScanPaths: String,
    ): List<Uri> {
        val songs = ArrayList<Uri>()
        Log.i(TAG, "------------ SCAN: Starting Full Scanner ------------")
        scannerState.value = 1
        scannerProgressProbe = 0
        scannerProgressTotal.value = 0
        scannerProgressCurrent.value = -1

        val scanPaths = uriListFromString(scanPaths)
        val excludedScanPaths = uriListFromString(excludedScanPaths)

        getScanFiles(scanPaths, excludedScanPaths, context).forEach { uri ->
            if (SCANNER_DEBUG)
                Log.v(TAG, "PATH: $uri")

            songs.add(uri)
        }

        scannerState.value = 0
        Log.i(TAG, "------------ SCAN: Finished Full Scanner ------------")
        return songs.toList()
    }

    /**
     * Update the Database with local files
     *
     * @param database
     * @param newSongs
     * @param matchStrength How lax should the scanner be
     * @param strictFileNames Whether to consider file names
     * @param refreshExisting Setting this this to true will updated existing songs
     * with new information, else existing song's data will not be touched, regardless
     * whether it was actually changed on disk
     *
     * Inserts a song if not found
     * Updates a song information depending on if refreshExisting value
     */
    fun syncDB(
        database: MusicDatabase,
        newSongs: java.util.ArrayList<SongTempData>,
        matchStrength: ScannerMatchCriteria,
        strictFileNames: Boolean,
        refreshExisting: Boolean = false,
        noDisable: Boolean = false
    ) {
        if (scannerState.value > 0) {
            Log.i(TAG, "------------ SYNC: Scanner in use. Aborting Local Library Sync ------------")
            return
        }
        Log.i(TAG, "------------ SYNC: Starting Local Library Sync ------------")
        scannerState.value = 3
//        scannerProgressProbe = 0 // using separate variable instead
        // deduplicate
        val finalSongs = ArrayList<SongTempData>()
        newSongs.forEach { song ->
            if (finalSongs.none { s -> compareSong(song.song, s.song, matchStrength, strictFileNames) }) {
                finalSongs.add(song)
            }
        }
        Log.d(TAG, "Entries to process: ${newSongs.size}. After dedup: ${finalSongs.size}")
        scannerProgressTotal.value = finalSongs.size
        scannerProgressCurrent.value = 0
        scannerProgressProbe = 0

        // sync
        var runs = 0
        finalSongs.forEach { song ->
            runs++
            if (SCANNER_DEBUG && runs % 20 == 0) {
                Log.d(TAG, "------------ SYNC: Local Library Sync: $runs/${finalSongs.size} processed ------------")
            }
            if (runs % 20 == 0) {
                scannerProgressCurrent.value += 20
            }

            if (scannerRequestCancel) {
                if (SCANNER_DEBUG)
                    Log.i(TAG, "WARNING: Requested to cancel Local Library Sync. Aborting.")
                scannerRequestCancel = false
                throw ScannerAbortException("Scanner canceled during Local Library Sync")
            }

            val querySong = database.searchSongsAllLocal(song.song.title)


            runBlocking(Dispatchers.IO) {
                // check if this song is known to the library
                val songMatch = querySong.first().filter {
                    return@filter compareSong(it, song.song, matchStrength, strictFileNames)
                }

                if (SCANNER_DEBUG) {
                    Log.v(
                        TAG,
                        "Found songs that match: ${songMatch.size}, Total results from database: ${querySong.first().size}"
                    )
                    if (songMatch.isNotEmpty()) {
                        Log.v(TAG, "FIRST Found songs ${songMatch.first().song.title}")
                    }
                }


                if (songMatch.isNotEmpty()) { // known song, update the song info in the database
                    if (SCANNER_DEBUG)
                        Log.v(TAG, "Found in database, updating song: ${song.song.title} rescan = $refreshExisting")

                    val oldSong = songMatch.first().song
                    val songToUpdate = song.song.song.copy(id = oldSong.id, localPath = song.song.song.localPath)

                    // don't run if we will update these values in rescan anyways
                    // always ensure inLibrary and local path values are valid
                    if (!refreshExisting && (oldSong.inLibrary == null || oldSong.localPath == null)) {
                        database.update(songToUpdate)

                        // update format
                        if (song.format != null) {
                            database.query {
                                upsert(song.format.copy(id = songToUpdate.id))
                            }
                        }
                    }

                    if (!refreshExisting) { // below is only for when rescan is enabled
                        // always update the path
                        database.updateLocalSongPath(songToUpdate.id, songToUpdate.inLibrary, songToUpdate.localPath)
                        return@runBlocking
                    }

                    database.transaction {
                        update(songToUpdate)

                        // destroy existing artist links
                        unlinkSongArtists(songToUpdate.id)
                        unlinkSongAlbums(songToUpdate.id)
                    }

                    // update artists
                    song.song.artists.forEachIndexed { index, it ->
                        val dbQuery =
                            database.artistLikeName(it.name).firstOrNull()?.sortedBy { item -> item.name.length }
                        val dbArtist = dbQuery?.let { item -> closestMatch(it.name, item) }

                        database.transaction {
                            if (dbArtist == null) {
                                // artist does not exist in db, add it then link it
                                insert(it)
                                insert(SongArtistMap(songToUpdate.id, it.id, index))
                            } else {
                                // artist does  exist in db, link to it
                                insert(SongArtistMap(songToUpdate.id, dbArtist.id, index))
                            }
                        }
                    }

                    song.song.genre?.forEachIndexed { index, it ->
                        val dbGenre = database.genreByAproxName(it.title).firstOrNull()?.firstOrNull()

                        database.transaction {
                            if (dbGenre == null) {
                                // genre does not exist in db, add it then link it
                                insert(it)
                                insert(SongGenreMap(songToUpdate.id, it.id, index))
                            } else {
                                // genre does exist in db, link to it
                                insert(SongGenreMap(songToUpdate.id, dbGenre.id, index))
                            }
                        }
                    }
                    /*
                    song.song.album?.let {
                        val dbQuery =
                            database.searchAlbums(it.title).firstOrNull()?.sortedBy { item -> item.album.title.length }
                        val dbAlbum = dbQuery?.let { item -> closestAlbumMatch(it.title, item) }

                        database.transaction {
                            if (dbAlbum == null) {
                                // album does not exist in db, add it then link it
                                insert(it)
                                insert(SongAlbumMap(songToUpdate.id, it.id, 0))
                            } else {
                                // album does  exist in db, link to it
                                insert(SongAlbumMap(songToUpdate.id, dbAlbum.album.id, dbAlbum.album.songCount))
                            }
                        }
                    }
                     */
                    // update format
                    if (song.format != null) {
                        database.query {
                            upsert(song.format.copy(id = songToUpdate.id))
                        }
                    }

                } else { // new song
                    if (SCANNER_DEBUG)
                        Log.v(TAG, "NOT found in database, adding song: ${song.song.title}")

                    database.transaction {
                        insert(song.song.toMediaMetadata())
                        song.format?.let {
                            upsert(it.copy(id = song.song.id))
                        }
                    }
                }
            }
        }

        // do not delete songs from database automatically, we just disable them
        if (!noDisable) {
            finalize(database)
            disableSongs(finalSongs.map { it.song }, database)
        }
        scannerState.value = 0
        Log.i(TAG, "------------ SYNC: Finished Local Library Sync ------------")
    }

    /**
     * A faster scanner implementation that adds new songs to the database,
     * and does not touch older songs entires (apart from removing
     * inacessable songs from libaray).
     *
     * No remote artist lookup is done
     *
     * WARNING: cachedDirectoryTree is not refreshed and may lead to inconsistencies.
     * It is highly recommend to rebuild the tree after scanner operation
     *
     * @param newSongs List of songs. This is expecting a barebones DirectoryTree
     * (only paths are necessary), thus you may use the output of refreshLocal().toList()
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun quickSync(
        database: MusicDatabase,
        newSongs: List<Uri>,
        matchCriteria: ScannerMatchCriteria,
        strictFileNames: Boolean,
    ) {
        Log.i(TAG, "------------ SYNC: Starting Quick (additive delta) Library Sync ------------")
        Log.d(TAG, "Entries to process: ${newSongs.size}")
        scannerState.value = 2
        scannerProgressTotal.value = newSongs.size
        scannerProgressCurrent.value = 0
        scannerProgressProbe = 0

        runBlocking(Dispatchers.IO) {
            // get list of all songs in db, then get songs unknown to the database
            val allSongs = database.allLocalSongs().first()
            val delta = newSongs.filterNot {
                allSongs.any { dbSong ->
                    val file = fileFromUri(context, it)?.absolutePath
                    if (file == null) return@any false
                    file == dbSong.song.localPath
                } // ignore user strictFileNames prefs for initial matching
            }

            val finalSongs = ArrayList<SongTempData>()
            val scannerJobs = ArrayList<Deferred<SongTempData?>>()
            runBlocking {
                // Get song basic metadata
                delta.forEach { s ->
                    if (scannerRequestCancel) {
                        Log.i(TAG, "WARNING: Requested to cancel. Aborting.")
                        scannerRequestCancel = false
                        throw ScannerAbortException("Scanner canceled during Quick (additive delta) Library Sync")
                    }
                    val sUri = fileFromUri(context, s)
                    if (sUri == null) throw ScannerCriticalFailureException("why null.")

                    val path = sUri
                    if (SCANNER_DEBUG)
                        Log.v(TAG, "PATH: $path")

                    /**
                     * TODO: do not link album (and whatever song id) with youtube yet, figure that out later
                     */

                    if (!SYNC_SCANNER) {
                        // use async scanner
                        scannerJobs.add(
                            async(scannerSession) {
                                var ret: SongTempData?
                                if (scannerRequestCancel) {
                                    Log.i(TAG, "WARNING: Canceling advanced scanner job.")
                                    throw ScannerAbortException("")
                                }
                                try {
                                    ret = advancedScan(s)
                                    scannerProgressProbe++
                                    if (SCANNER_DEBUG && scannerProgressProbe % 20 == 0) {
                                        Log.d(
                                            TAG,
                                            "------------ SCAN: Full Scanner: $scannerProgressProbe discovered ------------"
                                        )
                                    }
                                    if (scannerProgressProbe % 20 == 0) {
                                        scannerProgressCurrent.value = scannerProgressProbe
                                    }
                                } catch (e: InvalidAudioFileException) {
                                    ret = null
                                }
                                ret
                            }
                        )
                    } else {
                        // force synchronous scanning of songs. Do not catch errors
                        finalSongs.add(advancedScan(s))
                        scannerProgressProbe++
                        if (SCANNER_DEBUG && scannerProgressProbe % 5 == 0) {
                            Log.d(
                                TAG,
                                "------------ SCAN: Full Scanner: $scannerProgressProbe discovered ------------"
                            )
                        }
                        if (scannerProgressProbe % 5 == 0) {
                            scannerProgressCurrent.value = scannerProgressProbe
                        }
                    }
                }
            }

            if (!SYNC_SCANNER) {
                // use async scanner
                scannerJobs.awaitAll()
            }

            // add to finished list
            scannerJobs.forEach {
                val song = it.getCompleted()
                song?.song?.let { finalSongs.add(song) }
            }

            if (finalSongs.isNotEmpty()) {
                scannerState.value = 0
                syncDB(database, finalSongs, matchCriteria, strictFileNames, noDisable = true)
                scannerState.value = 2
            } else {
                Log.i(TAG, "Not syncing, no valid songs found!")
            }

            // we handle disabling songs here instead
            finalize(database)
            disableSongsByUri(newSongs, database)
        }

        scannerState.value = 0
        Log.i(TAG, "------------ SYNC: Finished Quick (additive delta) Library Sync ------------")
    }


    /**
     * Run a full scan and ful database update. This will update all song data in the
     * database of all songs, and also disable inacessable songs
     *
     * No remote artist lookup is done
     *
     * WARNING: cachedDirectoryTree is not refreshed and may lead to inconsistencies.
     * It is highly recommend to rebuild the tree after scanner operation
     *
     * @param newSongs List of songs. This is expecting a barebones DirectoryTree
     * (only paths are necessary), thus you may use the output of refreshLocal().toList()
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun fullSync(
        database: MusicDatabase,
        newSongs: List<Uri>,
        matchCriteria: ScannerMatchCriteria,
        strictFileNames: Boolean,
    ) {
        Log.i(TAG, "------------ SYNC: Starting FULL Library Sync ------------")
        Log.d(TAG, "Entries to process: ${newSongs.size}")
        scannerState.value = 2
        scannerProgressTotal.value = newSongs.size
        scannerProgressCurrent.value = 0
        scannerProgressProbe = 0

        runBlocking(Dispatchers.IO) {
            val finalSongs = ArrayList<SongTempData>()
            val scannerJobs = ArrayList<Deferred<SongTempData?>>()
            runBlocking {
                // Get song basic metadata
                newSongs.forEach { uri ->
                    if (scannerRequestCancel) {
                        Log.i(TAG, "WARNING: Requested to cancel. Aborting.")
                        scannerRequestCancel = false
                        throw ScannerAbortException("Scanner canceled during FULL Library Sync")
                    }

                    if (SCANNER_DEBUG)
                        Log.d(TAG, "PATH: $uri")

                    /**
                     * TODO: do not link album (and whatever song id) with youtube yet, figure that out later
                     */

                    if (!SYNC_SCANNER) {
                        // use async scanner
                        scannerJobs.add(
                            async(scannerSession) {
                                if (scannerRequestCancel) {
                                    Log.i(TAG, "WARNING: Canceling advanced scanner job.")
                                    throw ScannerAbortException("")
                                }
                                try {
                                    val ret = advancedScan(uri)
                                    scannerProgressProbe++
                                    if (SCANNER_DEBUG && scannerProgressProbe % 20 == 0) {
                                        Log.d(
                                            TAG,
                                            "------------ SCAN: Full Scanner: $scannerProgressProbe discovered ------------"
                                        )
                                    }
                                    if (scannerProgressProbe % 20 == 0) {
                                        scannerProgressCurrent.value = scannerProgressProbe
                                    }
                                    ret
                                } catch (e: InvalidAudioFileException) {
                                    null
                                }
                            }
                        )
                    } else {
                        // force synchronous scanning of songs. Do not catch errors
                        finalSongs.add(advancedScan(uri))
                    }
                }
            }

            if (!SYNC_SCANNER) {
                // use async scanner
                scannerJobs.awaitAll()
            }

            // add to finished list
            scannerJobs.forEach {
                val song = it.getCompleted()
                song?.song?.let { finalSongs.add(song) }
            }

            if (finalSongs.isNotEmpty()) {
                /**
                 * TODO: Delete all local format entity before scan
                 */
                scannerState.value = 0
                syncDB(database, finalSongs, matchCriteria, strictFileNames, refreshExisting = true)
                scannerState.value = 2
            } else {
                Log.i(TAG, "Not syncing, no valid songs found!")
            }
        }

        scannerState.value = 0
        Log.i(TAG, "------------ SYNC: Finished Quick (additive delta) Library Sync ------------")
    }


    /**
     * Converts all local artists to remote artists if possible
     */
    fun localToRemoteArtist(database: MusicDatabase) {
        if (scannerState.value > 0) {
            Log.i(TAG, "------------ SYNC: Scanner in use. Aborting youtubeArtistLookup job ------------")
            return
        }

        Log.i(TAG, "------------ SYNC: Starting youtubeArtistLookup job ------------")
        val prevScannerState = scannerState.value
        scannerState.value = 5
        runBlocking(Dispatchers.IO) {
            val allLocal = database.allLocalArtists().first()
            scannerProgressTotal.value = allLocal.size
            scannerProgressCurrent.value = 0
            scannerProgressProbe = 0

            allLocal.forEach { element ->
                val artistVal = element.name.trim()

                // check if this artist exists in DB already
                val databaseArtistMatch =
                    runBlocking(Dispatchers.IO) {
                        database.fuzzySearchArtists(artistVal).first().filter { artist ->
                            // only look for remote artists here
                            return@filter artist.name == artistVal && !artist.isLocal
                        }
                    }

                if (SCANNER_DEBUG)
                    Log.v(TAG, "ARTIST FOUND IN DB??? Results size: ${databaseArtistMatch.size}")

                // cancel here since this is where the real heavy action is
                if (scannerRequestCancel) {
                    Log.i(TAG, "WARNING: Requested to cancel youtubeArtistLookup job. Aborting.")
                    throw ScannerAbortException("Scanner canceled during youtubeArtistLookup job")
                }

                // resolve artist from YTM if not found in DB
                if (databaseArtistMatch.isEmpty()) {
                    try {
                        youtubeArtistLookup(artistVal)?.let {
                            // add new artist, switch all old references, then delete old one
                            database.insert(it)
                            try {
                                swapArtists(element, it, database)
                            } catch (e: Exception) {
                                reportException(e)
                            }
                        }
                    } catch (e: Exception) {
                        // don't touch anything if ytm fails --> keep old artist
                    }
                } else {
                    // swap with database artist
                    try {
                        swapArtists(element, databaseArtistMatch.first(), database)
                    } catch (e: Exception) {
                        reportException(e)
                    }
                }

                scannerProgressProbe++
                if (scannerProgressProbe % 20 == 0) {
                    scannerProgressCurrent.value = scannerProgressProbe
                }
                if (SCANNER_DEBUG && scannerProgressProbe % 20 == 0) {
                    Log.v(
                        TAG,
                        "------------ SYNC: youtubeArtistLookup job: $ scannerProgressCurrent.value/${scannerProgressTotal.value} artists processed ------------"
                    )
                }
            }

            if (scannerRequestCancel) {
                Log.i(TAG, "WARNING: Requested to cancel during localToRemoteArtist. Aborting.")
                throw ScannerAbortException("Scanner canceled during localToRemoteArtist")
            }
        }

        scannerState.value = prevScannerState
        Log.i(TAG, "------------ SYNC: youtubeArtistLookup job ended------------")
    }

    private fun disableSongsByUri(newSongs: List<Uri>, database: MusicDatabase) {
        Log.i(TAG, "Start finalize (disable songs) job. Number of valid songs: ${newSongs.size}")
        runBlocking(Dispatchers.IO) {
            // get list of all local songs in db
            database.disableInvalidLocalSongs() // make sure path is existing
            val allSongs = database.allLocalSongs().first()

            // disable if not in directory anymore
            for (song in allSongs) {
                if (song.song.localPath == null) {
                    continue
                }

                // new songs is all songs that are known to be valid
                // delete all songs in the DB that do not match a path
                if (newSongs.none { fileFromUri(context, it)?.absolutePath == song.song.localPath }) {
                    if (SCANNER_DEBUG)
                        Log.v(TAG, "Disabling song ${song.song.localPath}")
                    database.transaction {
                        disableLocalSong(song.song.id)
                    }
                }
            }
        }
    }

    private fun disableSongs(newSongs: List<Song>, database: MusicDatabase) {
        Log.i(TAG, "Start finalize (disable songs) job. Number of valid songs: ${newSongs.size}")
        runBlocking(Dispatchers.IO) {
            // get list of all local songs in db
            database.disableInvalidLocalSongs() // make sure path is existing
            val allSongs = database.allLocalSongs().first()

            // disable if not in directory anymore
            for (song in allSongs) {
                if (song.song.localPath == null) {
                    continue
                }

                // new songs is all songs that are known to be valid
                // delete all songs in the DB that do not match a path
                if (newSongs.none { it.song.localPath == song.song.localPath }) {
                    if (SCANNER_DEBUG)
                        Log.v(TAG, "Disabling song ${song.song.localPath}")
                    database.transaction {
                        disableLocalSong(song.song.id)
                    }
                }
            }
        }
    }

    /**
     * Remove inaccessible, and duplicate songs from the library
     */
    private fun finalize(database: MusicDatabase) {
        Log.i(TAG, "Start finalize (database cleanup job)")
        runBlocking(Dispatchers.IO) {
            // remove duplicates
            val dupes = database.duplicatedLocalSongs().first().toMutableList()
            var index = 0

            Log.d(TAG, "Start finalize (duplicate removal) job. Number of candidates: ${dupes.size}")
            while (index < dupes.size) {
                // collect all the duplicates
                val contenders = ArrayList<Pair<SongEntity, Int>>()
                val localPath = dupes[index].localPath
                while (index < dupes.size && dupes[index].localPath == localPath) {
                    contenders.add(Pair(dupes[index], database.getLifetimePlayCount(dupes[index].id).first()))
                    index++
                }
                // yeet the lower play count songs
                contenders.remove(contenders.maxByOrNull { it.second })
                contenders.forEach {
                    if (SCANNER_DEBUG)
                        Log.v(TAG, "Deleting song ${it.first.id} (${it.first.title})")
                    database.delete(it.first)
                }
            }

            // remove duplicated local artists
            val dbArtists: MutableList<Artist> = database.localArtistsByName().first().toMutableList()
            while (dbArtists.isNotEmpty()) {
                // gather same artists (precondition: artists are ordered by name
                val tmp = ArrayList<Artist>()
                val oldestArtist: Artist = dbArtists.removeAt(0)
                tmp.add(oldestArtist)
                while (dbArtists.isNotEmpty() && dbArtists.first().title == tmp.first().title) {
                    tmp.add(dbArtists.removeAt(0))
                }

                if (tmp.size > 1) {
                    // merge all duplicate artists into the oldest one
                    tmp.removeAt(0)
                    tmp.sortBy { it.artist.bookmarkedAt }
                    tmp.forEach { swapArtists(it.artist, oldestArtist.artist, database) }
                }
            }
        }
    }


    /**
     * Destroys all local library data (local songs and artists, does not include YTM downloads)
     * from the database
     */
    fun nukeLocalDB(database: MusicDatabase) {
        Log.w(TAG, "NUKING LOCAL FILE LIBRARY FROM DATABASE! Nuke status: ${database.nukeLocalData()}")
    }


    companion object {
        // do not put any thing that should adhere to the scanner lock in here
        const val TAG = "LocalMediaScanner"

        private var ownerId = -1
        private var localScanner: LocalMediaScanner? = null


        var scannerRequestCancel = false

        /**
         * -1: Inactive
         * 0: Idle
         * 1: Discovering (Crawling files)
         * 2: Scanning (Extract metadata and checking playability
         * 3: Syncing (Update database)
         * 4: Scan finished
         * 5: Ytm artist linking
         */
        var scannerState = MutableStateFlow(-1)
        var scannerProgressTotal = MutableStateFlow(-1)
        var scannerProgressCurrent = MutableStateFlow(-1)
        var scannerProgressProbe = -1


        /**
         * ==========================
         * Scanner management
         * ==========================
         */

        /**
         * Trust me bro, it should never be null
         */
        fun getScanner(context: Context, scannerImpl: ScannerImpl, owner: Int): LocalMediaScanner {

            if (localScanner == null) {
                // reset to taglib if ffMetadataEx disappears
                if (scannerImpl == ScannerImpl.FFMPEG_EXT && !ENABLE_FFMETADATAEX) {
                    CoroutineScope(Dispatchers.IO).launch {
                        context.dataStore.edit { settings ->
                            settings[ScannerImplKey] = ScannerImpl.TAGLIB.toString()
                            settings[AutomaticScannerKey] = false
                            // TODO: toast user maybe...?
                        }
                    }
                }
                localScanner = LocalMediaScanner(context, scannerImpl)
                scannerProgressTotal.value = 0
                scannerProgressCurrent.value = -1
                scannerProgressProbe = 0
            }

            ownerId = owner
            return localScanner!!
        }

        fun destroyScanner(owner: Int) {
            if (owner != ownerId && ownerId != -1) {
                Log.w(TAG, "Scanner instance can only be destroyed by the owner. Aborting. Check your ownerId.")
                return
            }
            ownerId = -1
            localScanner = null
            scannerState.value = -1
            scannerRequestCancel = false
            scannerProgressTotal.value = -1
            scannerProgressCurrent.value = -1
            scannerProgressProbe = -1

            Log.i(TAG, "Scanner instance destroyed")
        }


        /**
         * ==========================
         * Scanner extra scan utils
         * ==========================
         */


        /**
         * Build a list of files to scan, taking in exclusions into account. Exclusions
         * will override inclusions. All subdirectories will also be affected.
         *
         * Uri.path can be assumed to be non-null
         */
        fun getScanFiles(scanPaths: List<Uri>, excludedScanPaths: List<Uri>, context: Context): List<Uri> {
            val allSongs = ArrayList<Uri>()
            val resultingPaths =
                scanPaths.filterNot { incl ->
                    excludedScanPaths.any { excl -> incl.path?.startsWith(excl.path.toString()) == true }
                }

            resultingPaths.forEach { path ->
                try {
                    val file = documentFileFromUri(context, path)
                    if (file != null) {
                        val songsHere = ArrayList<DocumentFile>()
                        scanDfRecursive(file, songsHere) {
                            // we can expect lrc is not a song
                            // TODO: allowlist file ext, allow user to force scan for all files
                            val ext = it.substringAfterLast('.')
                            !(ext == "lrc" || ext == "ttml")
                        }

                        allSongs.addAll(songsHere.filterNot { incl ->
                            excludedScanPaths.any {
                                incl.uri.path?.startsWith(it.path.toString()) == true
                            }
                        }.map { it.uri })
                    }
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                    throw Exception("oh well idk man this should never happen")
                }
            }

            return allSongs.distinctBy { it.toString() }
        }

        fun scanDfRecursive(
            dir: DocumentFile,
            result: ArrayList<DocumentFile>,
            scanHidden: Boolean = false,
            validator: ((String) -> Boolean)? = null
        ): DocumentFile? {
            val files = dir.listFiles()
            for (file in files) {
                if (!scanHidden && file.name?.startsWith(".") == true) continue
                if (file.isDirectory && (scanHidden || !file.listFiles().any { it.name == ".nomedia" })) {
                    // look into subdirs
                    scanDfRecursive(file, result, scanHidden, validator)
                } else {
                    val name = file.name ?: continue
                    // add if file matches
                    if (validator == null || validator(name)) {
                        result.add(file)
                        scannerProgressProbe++
                        if (scannerProgressProbe % 20 == 0) {
                            scannerProgressTotal.value = scannerProgressProbe
                        }
                    }
                }
            }
            return null
        }

        /**
         * Quickly rebuild a skeleton directory tree of local files based on the database
         *
         * Notes:
         * If files move around, that's on you to re run the scanner.
         * If the metadata changes, that's also on you to re run the scanner.
         *
         * @param scanPaths List of whitelist paths to scan under. This assumes
         * the current directory is /storage/emulated/0/ a.k.a, /sdcard.
         * For example, to scan under Music and Documents/songs --> ("Music", Documents/songs)
         * @param filter Raw file path
         */
        suspend fun refreshLocal(
            context: Context,
            database: MusicDatabase,
            filter: String
        ): DirectoryTree {
            val newDirectoryStructure = DirectoryTree(filter.trimEnd { it == '/' }, CulmSongs(0))

            // get songs from db
            var existingSongs: List<Song>
            runBlocking {
                existingSongs = database.localSongsInDirShallow(filter).first()
            }

            Log.i(TAG, "------------ SCAN: Starting Quick Directory Rebuild ------------")

            // Build directory tree with existing files
            existingSongs.forEach { s ->
                val path = s.song.localPath ?: return@forEach
                val filterPath =
                    (if (path.startsWith(filter)) path.substringAfter(filter) else path).trimStart { it == '/' }
                newDirectoryStructure.insert(filterPath, s)
            }

            Log.i(TAG, "------------ SCAN: Finished Quick Directory Rebuild ------------")
            return newDirectoryStructure.androidStorageWorkaround()
        }


        /**
         * ==========================
         * Scanner helpers
         * ==========================
         */


        /**
         * Check if artists are the same
         *
         *  Both null == same artists
         *  Either null == different artists
         */
        fun compareArtist(a: List<ArtistEntity>, b: List<ArtistEntity>): Boolean {
            if (a.isEmpty() && b.isEmpty()) {
                return true
            } else if (a.isEmpty() || b.isEmpty()) {
                return false
            }

            // compare entries
            if (a.size != b.size) {
                return false
            }
            val matchingArtists = a.filter { artist ->
                b.any { it.name.lowercase(Locale.getDefault()) == artist.name.lowercase(Locale.getDefault()) }
            }

            return matchingArtists.size == a.size
        }

        /**
         * Check the similarity of a song
         *
         * @param a
         * @param b
         * @param matchStrength How lax should the scanner be
         * @param strictFileNames Whether to consider file names
         */
        fun compareSong(
            a: Song,
            b: Song,
            matchStrength: ScannerMatchCriteria = ScannerMatchCriteria.LEVEL_2,
            strictFileNames: Boolean = false
        ): Boolean {
            // if match file names
            if (strictFileNames &&
                (a.song.localPath?.substringAfterLast('/') !=
                        b.song.localPath?.substringAfterLast('/'))
            ) {
                return false
            }

            /**
             * Compare file paths
             *
             * I draw the "user error" line here
             */
            fun closeEnough(): Boolean {
                return a.song.localPath == b.song.localPath
            }

            // compare songs based on scanner strength
            return when (matchStrength) {
                ScannerMatchCriteria.LEVEL_1 -> a.song.title == b.song.title
                ScannerMatchCriteria.LEVEL_2 -> closeEnough() || (a.song.title == b.song.title &&
                        compareArtist(a.artists, b.artists))

                ScannerMatchCriteria.LEVEL_3 -> closeEnough() || (a.song.title == b.song.title &&
                        compareArtist(a.artists, b.artists) /* && album compare goes here */)
            }
        }

        /**
         * Search for an artist on YouTube Music.
         */
        fun youtubeSongLookup(query: String, songUrl: String?): List<MediaMetadata> {
            var ytmResult = ArrayList<MediaMetadata>()

            runBlocking(Dispatchers.IO) {
                var exactSong: SongItem? = null
                if (songUrl != null) {
                    runBlocking(Dispatchers.IO) {
                        runCatching {
                            YouTube.queue(listOf(songUrl.substringAfter("/watch?v=").substringBefore("&")))
                        }.onSuccess {
                            exactSong = it.getOrNull()?.firstOrNull()
                        }.onFailure {
                            reportException(it)
                        }
                    }
                }

                // prefer song from url
                if (exactSong != null) {
                    ytmResult.add(exactSong.toMediaMetadata())
                    if (SCANNER_DEBUG)
                        Log.v(TAG, "Found exact song: ${exactSong.title} [${exactSong.id}]")
                    return@runBlocking
                }
                YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).onSuccess { result ->

                    val foundSong = result.items.filter {
                        // TODO: might want to implement proper matching to remove outlandish results
                        it is SongItem
                    }
                    ytmResult.addAll(foundSong.map { (it as SongItem).toMediaMetadata() })

                    if (SCANNER_DEBUG)
                        Log.v(TAG, "Remote song: ${foundSong.firstOrNull()?.title} [${foundSong.firstOrNull()?.id}]")
                }.onFailure {
                    throw Exception("Failed to search on YouTube Music: ${it.message}")
                }

            }

            return ytmResult
        }

        /**
         * Search for an artist on YouTube Music.
         *
         * If no artist is found, create one locally
         */
        fun youtubeArtistLookup(query: String): ArtistEntity? {
            var ytmResult: ArtistEntity? = null

            // hit up YouTube for artist
            runBlocking(Dispatchers.IO) {
                YouTube.search(query, YouTube.SearchFilter.FILTER_ARTIST).onSuccess { result ->

                    val foundArtist = result.items.filter { it is ArtistItem }.firstOrNull {
                        // TODO: might want to implement smarter matching
                        it.title.lowercase(Locale.getDefault()) == query.lowercase(Locale.getDefault())
                    } as ArtistItem? ?: throw Exception("Failed to search: Artist not found on YouTube Music")
                    ytmResult = ArtistEntity(
                        foundArtist.id,
                        foundArtist.title,
                        foundArtist.thumbnail,
                        foundArtist.channelId
                    )

                    if (SCANNER_DEBUG)
                        Log.v(TAG, "Found remote artist:  ${foundArtist.title} [${foundArtist.id}]")
                }.onFailure {
                    throw Exception("Failed to search on YouTube Music")
                }

            }

            return ytmResult
        }

        /**
         * Swap all participation(s) with old artist to use new artist
         *
         * p.s. This is here instead of DatabaseDao because it won't compile there because
         * "oooga boooga error in generated code"
         */
        fun swapArtists(old: ArtistEntity, new: ArtistEntity, database: MusicDatabase) {
            if (database.artistById(old.id) == null) {
                throw Exception("Attempting to swap with non-existent old artist in database with id: ${old.id}")
            }
            if (database.artistById(new.id) == null) {
                throw Exception("Attempting to swap with non-existent new artist in database with id: ${new.id}")
            }

            database.transaction {
                // update participation(s)
                database.updateSongArtistMap(old.id, new.id)
                database.updateAlbumArtistMap(old.id, new.id)

                // nuke old artist
                database.safeDeleteArtist(old.id)
            }
        }
    }
}

class InvalidAudioFileException(message: String) : Throwable(message)
class ScannerAbortException(message: String) : Throwable(message)
class ScannerCriticalFailureException(message: String) : Throwable(message)

// remove if building with the submodule
class FFMpegScanner() : MetadataScanner {

    override fun getAllMetadataFromFile(file: File): SongTempData {
        throw NotImplementedError()
    }
}

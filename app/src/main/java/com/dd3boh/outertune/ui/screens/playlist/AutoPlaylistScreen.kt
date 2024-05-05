package com.dd3boh.outertune.ui.screens.playlist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.OfflinePin
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastSumBy
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import com.dd3boh.outertune.LocalDatabase
import com.dd3boh.outertune.LocalDownloadUtil
import com.dd3boh.outertune.LocalPlayerAwareWindowInsets
import com.dd3boh.outertune.LocalPlayerConnection
import com.dd3boh.outertune.R
import com.dd3boh.outertune.constants.AlbumThumbnailSize
import com.dd3boh.outertune.constants.ThumbnailCornerRadius
import com.dd3boh.outertune.db.entities.PlaylistEntity
import com.dd3boh.outertune.db.entities.Song
import com.dd3boh.outertune.extensions.toMediaItem
import com.dd3boh.outertune.extensions.togglePlayPause
import com.dd3boh.outertune.playback.ExoDownloadService
import com.dd3boh.outertune.playback.queues.ListQueue
import com.dd3boh.outertune.ui.component.AutoResizeText
import com.dd3boh.outertune.ui.component.DefaultDialog
import com.dd3boh.outertune.ui.component.EmptyPlaceholder
import com.dd3boh.outertune.ui.component.FontSizeRange
import com.dd3boh.outertune.ui.component.LocalMenuState
import com.dd3boh.outertune.ui.component.SongListItem
import com.dd3boh.outertune.ui.component.SwipeToQueueBox
import com.dd3boh.outertune.ui.menu.SongMenu
import com.dd3boh.outertune.utils.makeTimeString
import com.dd3boh.outertune.viewmodels.AutoPlaylistViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AutoPlaylistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: AutoPlaylistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val songs by viewModel.songs.collectAsState()

    val playlistId = viewModel.playlistId
    val playlist = PlaylistEntity(
        id = playlistId,
        name = when (playlistId) {
            "liked" -> stringResource(id = R.string.liked_songs)
            else -> ""
        },
        browseId = when (playlistId) {
            "liked" -> "LM"
            else -> null
        },
    )

    val thumbnail by viewModel.thumbnail.collectAsState()
    val mutableSongs = remember { mutableStateListOf<Song>() }

    val snackbarHostState = remember { SnackbarHostState() }
    val lazyListState = rememberLazyListState()

    val playlistLength = remember(songs) {
        songs.fastSumBy { it.song.duration }
    }

    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember {
        mutableIntStateOf(Download.STATE_STOPPED)
    }

    LaunchedEffect(songs) {
        mutableSongs.apply {
            clear()
            addAll(songs)
        }
        if (songs.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs.all { downloads[it.song.id]?.state == Download.STATE_COMPLETED })
                    Download.STATE_COMPLETED
                else if (songs.all {
                        downloads[it.song.id]?.state == Download.STATE_QUEUED
                                || downloads[it.song.id]?.state == Download.STATE_DOWNLOADING
                                || downloads[it.song.id]?.state == Download.STATE_COMPLETED
                    })
                    Download.STATE_DOWNLOADING
                else
                    Download.STATE_STOPPED
        }
    }

    var showRemoveDownloadDialog by remember {
        mutableStateOf(false)
    }

    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = {
                Text(
                    text = stringResource(R.string.remove_download_playlist_confirm, playlist.name),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
            },
            buttons = {
                TextButton(
                    onClick = { showRemoveDownloadDialog = false }
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }

                TextButton(
                    onClick = {
                        showRemoveDownloadDialog = false
                        database.transaction {
                            clearPlaylist(playlist.id)
                        }

                        songs.forEach { song ->
                            DownloadService.sendRemoveDownload(
                                context,
                                ExoDownloadService::class.java,
                                song.song.id,
                                false
                            )
                        }
                    }
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            }
        )
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
        ) {
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(AlbumThumbnailSize)
                                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
                                    shape = RoundedCornerShape(ThumbnailCornerRadius))
                        ) {
                            Icon(
                                imageVector = thumbnail,
                                contentDescription = null,
                                tint = LocalContentColor.current.copy(alpha = 0.8f),
                                modifier = Modifier
                                    .size(AlbumThumbnailSize / 2 + 16.dp)
                                    .align(Alignment.Center)
                            )
                        }

                        Column(
                            verticalArrangement = Arrangement.Center,
                        ) {
                            AutoResizeText(
                                text = playlist.name,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                fontSizeRange = FontSizeRange(16.sp, 22.sp)
                            )

                            Text(
                                text = pluralStringResource(R.plurals.n_song, songs.size, songs.size),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Normal
                            )

                            Text(
                                text = makeTimeString(playlistLength * 1000L),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Normal
                            )

                            Row {
                                if (songs.isNotEmpty()) {
                                    when (downloadState) {
                                        Download.STATE_COMPLETED -> {
                                            IconButton(
                                                onClick = {
                                                    showRemoveDownloadDialog = true
                                                }
                                            ) {
                                                Icon(
                                                    Icons.Rounded.OfflinePin,
                                                    contentDescription = null
                                                )
                                            }
                                        }

                                        Download.STATE_DOWNLOADING -> {
                                            IconButton(
                                                onClick = {
                                                    songs.forEach { song ->
                                                        DownloadService.sendRemoveDownload(
                                                            context,
                                                            ExoDownloadService::class.java,
                                                            song.song.id,
                                                            false
                                                        )
                                                    }
                                                }
                                            ) {
                                                CircularProgressIndicator(
                                                    strokeWidth = 2.dp,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }

                                        else -> {
                                            IconButton(
                                                onClick = {
                                                    songs.forEach { song ->
                                                        val downloadRequest =
                                                            DownloadRequest.Builder(
                                                                song.song.id,
                                                                song.song.id.toUri()
                                                            )
                                                                .setCustomCacheKey(song.song.id)
                                                                .setData(song.song.title.toByteArray())
                                                                .build()
                                                        DownloadService.sendAddDownload(
                                                            context,
                                                            ExoDownloadService::class.java,
                                                            downloadRequest,
                                                            false
                                                        )
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    Icons.Rounded.Download,
                                                    contentDescription = null
                                                )
                                            }
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            playerConnection.addToQueue(
                                                items = songs.map { it.toMediaItem() }
                                            )
                                        }
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Rounded.QueueMusic,
                                            contentDescription = null
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (songs.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = {
                                    playerConnection.playQueue(
                                        ListQueue(
                                            title = playlist.name,
                                            items = songs.map { it.toMediaItem() },
                                            playlistId = playlist.browseId
                                        )
                                    )
                                },
                                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(ButtonDefaults.IconSize)
                                )
                                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                Text(stringResource(R.string.play))
                            }

                            OutlinedButton(
                                onClick = {
                                    playerConnection.playQueue(
                                        ListQueue(
                                            title = playlist.name,
                                            items = songs.shuffled()
                                                .map { it.toMediaItem() },
                                            playlistId = playlist.browseId
                                        )
                                    )
                                },
                                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Rounded.Shuffle,
                                    contentDescription = null,
                                    modifier = Modifier.size(ButtonDefaults.IconSize)
                                )
                                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                Text(stringResource(R.string.shuffle))
                            }
                        }
                    }
                }
            }

            if (songs.isEmpty()) {
                item {
                    EmptyPlaceholder(
                        icon = Icons.Rounded.MusicNote,
                        text = stringResource(R.string.playlist_is_empty)
                    )
                }
            }

            itemsIndexed(
                items = mutableSongs,
                key = { _, song -> song.id }
            ) { index, song ->
                SwipeToQueueBox(
                    item = song.toMediaItem(),
                    content = {
                        SongListItem(
                            song = song,
                            isActive = song.song.id == mediaMetadata?.id,
                            isPlaying = isPlaying,
                            showInLibraryIcon = true,
                            showLikedIcon = false,
                            trailingContent = {
                                IconButton(
                                    onClick = {
                                        menuState.show {
                                            SongMenu(
                                                originalSong = song,
                                                playlistBrowseId = playlist.browseId,
                                                navController = navController,
                                                onDismiss = menuState::dismiss
                                            )
                                        }
                                    }
                                ) {
                                    Icon(
                                        Icons.Rounded.MoreVert,
                                        contentDescription = null
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                                .combinedClickable(
                                    onClick = {
                                        if (song.song.id == mediaMetadata?.id) {
                                            playerConnection.player.togglePlayPause()
                                        } else {
                                            playerConnection.playQueue(
                                                ListQueue(
                                                    title = playlist.name,
                                                    items = songs.map { it.toMediaItem() },
                                                    startIndex = index,
                                                    playlistId = playlist.browseId
                                                )
                                            )
                                        }
                                    },
                                    onLongClick = {
                                        menuState.show {
                                            SongMenu(
                                                originalSong = song,
                                                playlistBrowseId = playlist.browseId,
                                                navController = navController,
                                                onDismiss = menuState::dismiss
                                            )
                                        }
                                    }
                                )
                        )
                    },
                    snackbarHostState = snackbarHostState
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
                .align(Alignment.BottomCenter)
        )
    }
}
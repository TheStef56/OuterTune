package com.dd3boh.outertune.ui.screens.unavailablesongs

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.dd3boh.outertune.LocalDatabase
import com.dd3boh.outertune.LocalPlayerAwareWindowInsets
import com.dd3boh.outertune.LocalPlayerConnection
import com.dd3boh.outertune.R
import com.dd3boh.outertune.constants.AppBarHeight
import com.dd3boh.outertune.constants.SearchSource
import com.dd3boh.outertune.constants.TopBarInsets
import com.dd3boh.outertune.db.entities.ArtistEntity
import com.dd3boh.outertune.db.entities.Song
import com.dd3boh.outertune.db.entities.SongEntity
import com.dd3boh.outertune.extensions.togglePlayPause
import com.dd3boh.outertune.models.toMediaMetadata
import com.dd3boh.outertune.playback.queues.ListQueue
import com.dd3boh.outertune.ui.component.ChipsRow
import com.dd3boh.outertune.ui.component.EmptyPlaceholder
import com.dd3boh.outertune.ui.component.SearchBar
import com.dd3boh.outertune.ui.component.button.IconButton
import com.dd3boh.outertune.ui.component.items.UnavailableSongSearchListItem
import com.dd3boh.outertune.ui.component.items.YouTubeListItem
import com.dd3boh.outertune.ui.component.shimmer.ListItemPlaceHolder
import com.dd3boh.outertune.ui.component.shimmer.ShimmerHost
import com.dd3boh.outertune.viewmodels.UnavailableSong
import com.dd3boh.outertune.viewmodels.UnavailableSongsViewModel
import com.metrolist.innertube.YouTube.SearchFilter.Companion.FILTER_SONG
import com.metrolist.innertube.YouTube.SearchFilter.Companion.FILTER_VIDEO
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.YTItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnavailableSongSearch(
    navController: NavController,
    viewModel: UnavailableSongsViewModel,
    scrollBehavior: TopAppBarScrollBehavior,
    backStackEntry: NavBackStackEntry
) {
    val searchBarFocusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        ChipsRow(
            chips = listOf(
                FILTER_SONG to stringResource(R.string.filter_songs),
                FILTER_VIDEO to stringResource(R.string.filter_videos),
            ),
            currentValue = viewModel.searchFilter.value,
            onValueUpdate = {
                viewModel.searchFilter.value = it
                viewModel.search()
            },
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top).add(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal)))
                .padding(top = AppBarHeight)
        )

        SearchBar(
            query = viewModel.searchQuery.value,
            onQueryChange = {
                viewModel.onSearchQueryChange(it)
            },
            onSearch = {
                viewModel.onSearchQueryChange(TextFieldValue(it))
                Log.d("SEARCH:", "${viewModel.onlineResult.value?.items?.size}")
            },
            active = true,
            onActiveChange = { notShouldExit ->
                if (!notShouldExit) navController.popBackStack()
            },
            scrollBehavior = scrollBehavior,
            placeholder = {
                Text(
                    text = stringResource(
                        when (viewModel.searchSource.value) {
                            SearchSource.LOCAL -> R.string.search_library
                            SearchSource.ONLINE -> R.string.search_yt_music
                        }
                    )
                )
            },
            trailingIcon = {
                if (viewModel.searchQuery.value.text.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.onSearchQueryChange(TextFieldValue()) }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = null
                        )
                    }
                }
                IconButton(
                    onClick = {
                        viewModel.searchSource.value =
                            if (viewModel.searchSource.value == SearchSource.ONLINE) SearchSource.LOCAL else SearchSource.ONLINE
                    }
                ) {
                    Icon(
                        imageVector = when (viewModel.searchSource.value) {
                            SearchSource.LOCAL -> Icons.Rounded.LibraryMusic
                            SearchSource.ONLINE -> Icons.Rounded.Language
                        },
                        contentDescription = null
                    )
                }
            },
            windowInsets = WindowInsets(0),
            focusRequester = searchBarFocusRequester,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxSize()
        ) {
            fun swapSong(newSong: Song) {
                CoroutineScope(Dispatchers.IO).launch {
                    val songId = backStackEntry.arguments?.getString("id") ?: "None"
                    val old = if (songId == "None") {
                        viewModel.unavailableSongs[viewModel.toSwapIndex.intValue]
                    } else {
                        val song = database.song(songId).first()
                        val query = "${song!!.title} ${Uri.decode(song.artists.joinToString(" ") {it.name})}"
                        UnavailableSong(
                            query = query,
                            song = song,
                            uuid = UUID.randomUUID().toString()
                        )
                    }
                    database.delete(newSong.song)
                    database.changeSongId(old.song.id, newSong.id)
                    val newSongSong = Song(
                        song = SongEntity(
                            id = newSong.id,
                            title = newSong.song.title,
                            duration = newSong.song.duration,
                            thumbnailUrl = newSong.song.thumbnailUrl,
                            inLibrary = old.song.song.inLibrary,
                            isLocal = newSong.song.isLocal,
                            localPath = newSong.song.localPath,
                            dateDownload = old.song.song.dateDownload,
                            liked = old.song.song.liked,
                            likedDate = old.song.song.likedDate,
                            trackNumber = newSong.song.trackNumber,
                            discNumber = newSong.song.discNumber,
                            albumId = newSong.song.albumId,
                            albumName = newSong.song.albumName,
                            year = old.song.song.year,
                            date = old.song.song.date,
                            dateModified = old.song.song.dateModified,
                        ),
                        artists = newSong.artists,
                        album = newSong.album,
                        genre = newSong.genre
                    )
                    if (songId == "None") {
                        viewModel.unavailableSongs[viewModel.toSwapIndex.intValue] =
                            UnavailableSong(
                                query = old.query,
                                song = newSongSong,
                                uuid = old.uuid,
                            )
                    }
                    database.update(newSongSong.song)
                }
            }

            Crossfade(
                targetState = viewModel.searchSource.value,
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
                                        swapSong(item)
                                        navController.popBackStack()
                                        Toast.makeText(context, R.string.replaced_one_with_search, Toast.LENGTH_SHORT).show()
                                        viewModel.onSearchQueryChange(TextFieldValue())
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
                                    swapSong(song)
                                    navController.popBackStack()
                                    Toast.makeText(context, R.string.replaced_one_with_search, Toast.LENGTH_SHORT).show()
                                    viewModel.onSearchQueryChange(TextFieldValue())
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

                                if (onlineResults!!.continuation != null && viewModel.searchQuery.value.text != "") {
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

                            if (viewModel.searchQuery.value.text != "" && (onlineResults == null || onlineResults!!.continuation != null)) {
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
    TopAppBar(
        title = {
            Text(stringResource(R.string.swap_song))
        },
        actions = {
        },
        navigationIcon = {
            IconButton(
                onClick = {
                    navController.popBackStack()
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
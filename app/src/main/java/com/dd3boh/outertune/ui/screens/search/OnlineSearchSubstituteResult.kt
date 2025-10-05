package com.dd3boh.outertune.ui.screens.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.dd3boh.outertune.LocalPlayerAwareWindowInsets
import com.dd3boh.outertune.LocalPlayerConnection
import com.dd3boh.outertune.LocalSnackbarHostState
import com.dd3boh.outertune.R
import com.dd3boh.outertune.constants.SearchFilterHeight
import com.dd3boh.outertune.constants.SwipeToQueueKey
import com.dd3boh.outertune.db.entities.ArtistEntity
import com.dd3boh.outertune.db.entities.Song
import com.dd3boh.outertune.extensions.toMediaItem
import com.dd3boh.outertune.extensions.togglePlayPause
import com.dd3boh.outertune.models.toMediaMetadata
import com.dd3boh.outertune.playback.queues.ListQueue
import com.dd3boh.outertune.ui.component.EmptyPlaceholder
import com.dd3boh.outertune.ui.component.LazyColumnScrollbar
import com.dd3boh.outertune.ui.component.SwipeToQueueBox
import com.dd3boh.outertune.ui.component.button.IconButton
import com.dd3boh.outertune.ui.component.items.YouTubeListItem
import com.dd3boh.outertune.ui.component.shimmer.ListItemPlaceHolder
import com.dd3boh.outertune.ui.component.shimmer.ShimmerHost
import com.dd3boh.outertune.utils.rememberPreference
import com.dd3boh.outertune.viewmodels.OnlineSearchSubstituteViewModel
import com.zionhuang.innertube.YouTube
import com.zionhuang.innertube.models.SongItem
import com.zionhuang.innertube.models.YTItem
import java.net.URLDecoder

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnlineSearchSubstituteResult(
    navController: NavController,
    substituteSong: MutableState<Song?>,
    viewModel: OnlineSearchSubstituteViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val swipeEnabled by rememberPreference(SwipeToQueueKey, true)

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val lazyListState = rememberLazyListState()
    val snackbarHostState = LocalSnackbarHostState.current

    val itemsPage by remember(YouTube.SearchFilter.FILTER_SONG) {
        derivedStateOf {
                viewModel.viewStateMap[YouTube.SearchFilter.FILTER_SONG.value]
        }
    }

    LaunchedEffect(lazyListState) {
        snapshotFlow {
            lazyListState.layoutInfo.visibleItemsInfo.any { it.key == "loading" }
        }.collect { shouldLoadMore ->
            if (!shouldLoadMore) return@collect
            viewModel.loadMore()
        }
    }

    val ytItemContent: @Composable LazyItemScope.(YTItem, List<YTItem>) -> Unit =
        { item: YTItem, collection: List<YTItem> ->
            val content: @Composable () -> Unit = {
                YouTubeListItem(
                    item = item,
                    isActive = mediaMetadata?.id == item.id ,
                    isPlaying = isPlaying,
                    trailingContent = {
                        IconButton(
                            onClick = {
                                val songItem = item as SongItem
                                val mediaData = songItem.toMediaMetadata()
                                substituteSong.value = Song(
                                    song = mediaData.toSongEntity(),
                                    artists = mediaData.artists.map {
                                        ArtistEntity(
                                            id = it.id ?: ArtistEntity.generateArtistId(),
                                            name = it.name
                                        )
                                    }
                                )
                                navController.popBackStack()
                            }
                        ) {
                            Icon(
                                Icons.Rounded.SwapHoriz,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier
                        .combinedClickable(
                            onClick = {
                                if (item.id == mediaMetadata?.id) {
                                    playerConnection.player.togglePlayPause()
                                } else {
                                    val songSuggestions = collection.filter { it is SongItem }
                                    playerConnection.playQueue(
                                        ListQueue(
                                            title = "${context.getString(R.string.queue_searched_songs_ot)} ${
                                                URLDecoder.decode(
                                                    viewModel.query,
                                                    "UTF-8"
                                                )
                                            }",
                                            items = songSuggestions.map { (it as SongItem).toMediaMetadata() },
                                            startIndex = songSuggestions.indexOf(item)
                                        ),
                                        replace = true,
                                    )
                                }
                            },
                        )
                        .animateItem()
                )
            }

            if (item !is SongItem) content()
            else SwipeToQueueBox(
                item = item.toMediaItem(),
                swipeEnabled = swipeEnabled,
                snackbarHostState = snackbarHostState,
                content = { content() },
            )
        }

    LazyColumn(
        state = lazyListState,
        contentPadding = LocalPlayerAwareWindowInsets.current
            .add(WindowInsets(top = SearchFilterHeight))
            .asPaddingValues()
    ) {

            items(
                items = itemsPage?.items.orEmpty(),
                key = { it.id }
            ) { item ->
                ytItemContent(item, itemsPage?.items.orEmpty())
            }

            if (itemsPage?.continuation != null) {
                item(key = "loading") {
                    ShimmerHost {
                        repeat(3) {
                            ListItemPlaceHolder()
                        }
                    }
                }
            }

            if (itemsPage?.items?.isEmpty() == true) {
                item {
                    EmptyPlaceholder(
                        icon = Icons.Rounded.Search,
                        text = stringResource(R.string.no_results_found),
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }

    LazyColumnScrollbar(
        state = lazyListState,
    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
                .align(Alignment.BottomCenter)
        )
    }

}

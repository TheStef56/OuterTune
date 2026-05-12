package com.dd3boh.outertune.ui.menu

import android.content.Intent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastSumBy
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.dd3boh.outertune.LocalDatabase
import com.dd3boh.outertune.LocalSyncUtils
import com.dd3boh.outertune.R
import com.dd3boh.outertune.constants.ListThumbnailSize
import com.dd3boh.outertune.constants.ThumbnailCornerRadius
import com.dd3boh.outertune.db.entities.Song
import com.dd3boh.outertune.models.MediaMetadata
import com.dd3boh.outertune.models.toMediaMetadata
import com.dd3boh.outertune.ui.component.button.IconButton
import com.dd3boh.outertune.ui.component.items.ListItem
import com.dd3boh.outertune.ui.dialog.ArtistDialog
import com.dd3boh.outertune.ui.dialog.DetailsDialog
import com.dd3boh.outertune.ui.screens.Screens
import com.dd3boh.outertune.utils.joinByBullet
import com.dd3boh.outertune.utils.makeTimeString
import com.dd3boh.outertune.viewmodels.ImportM3uViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun ImportSongMenu(
    song: Song,
    modelIndex: Pair<ImportM3uViewModel, Int>,
    navController: NavController,
    m3uNavController: NavController,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val density = LocalDensity.current
    val clipboardManager = LocalClipboard.current
    val syncUtils = LocalSyncUtils.current

    val currentFormatState = database.format(song.id).collectAsState(initial = null)
    val currentFormat = currentFormatState.value

    var showSelectArtistDialog by rememberSaveable {
        mutableStateOf(false)
    }
    var showDetailsDialog by rememberSaveable {
        mutableStateOf(false)
    }

    ListItem(
        title = song.song.title,
        subtitle = joinByBullet(
            song.artists.joinToString { it.name },
            makeTimeString(song.song.duration * 1000L)
        ),
        thumbnailContent = {
            val px = (ListThumbnailSize.value * density.density).roundToInt()
            AsyncImage(
                model = song.song.getThumbnailModel(px, px),
                contentDescription = null,
                modifier = Modifier
                    .size(ListThumbnailSize)
                    .clip(RoundedCornerShape(ThumbnailCornerRadius))
            )
        },
        trailingContent = {
            IconButton(
                onClick = {
                    val s = song.song.toggleLike()
                    CoroutineScope(Dispatchers.IO).launch {
                        database.insert(s)
                        database.query {
                            update(s)
                        }
                    }

                    if (!s.isLocal) {
                        syncUtils.likeSong(s)
                    }
                }
            ) {
                Icon(
                    painter = painterResource(if (database.song(song.id).collectAsState(initial = null).value?.song?.liked
                            ?: false) R.drawable.favorite else R.drawable.favorite_border),
                    tint = if (song.song.liked) MaterialTheme.colorScheme.error else LocalContentColor.current,
                    contentDescription = null
                )
            }
        }
    )

    HorizontalDivider()

    GridMenu(
        contentPadding = PaddingValues(
            start = 8.dp,
            top = 8.dp,
            end = 8.dp,
            bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()
        )
    ) {

        GridMenuItem(
            icon = R.drawable.artist,
            title = R.string.view_artist
        ) {
            if (song.artists.size == 1) {
                navController.navigate("artist/${song.artists[0].id}")
                onDismiss()
            } else {
                showSelectArtistDialog = true
            }
        }
        if (song.song.albumId != null && !song.song.isLocal) {
            GridMenuItem(
                icon = Icons.Rounded.Album,
                title = R.string.view_album
            ) {
                onDismiss()
                navController.navigate("album/${song.song.albumId}")
            }
        }
        if (!song.song.isLocal) {
            GridMenuItem(
                icon = Icons.Rounded.Share,
                title = R.string.share
            ) {
                onDismiss()
                val intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/watch?v=${song.id}")
                }
                context.startActivity(Intent.createChooser(intent, null))
            }
        }
        GridMenuItem(
            icon = Icons.Rounded.Info,
            title = R.string.details
        ) {
            showDetailsDialog = true
        }

        GridMenuItem(
            icon = Icons.Rounded.SwapHoriz,
            title = R.string.swap_song
        ) {
            onDismiss()
            m3uNavController.navigate(Screens.M3uSearch.route)
        }

        GridMenuItem(
            icon = Icons.Rounded.Delete,
            title = R.string.delete_import
        ) {
            onDismiss()
            modelIndex.first.importedSongs.removeAt(modelIndex.second)
        }
    }

    /**
     * ---------------------------
     * Dialogs
     * ---------------------------
     */


    if (showSelectArtistDialog) {
        ArtistDialog(
            navController = navController,
            artists = song.artists,
            onDismiss = { showSelectArtistDialog = false }
        )
    }

    if (showDetailsDialog) {
        DetailsDialog(
            mediaMetadata = song.toMediaMetadata(),
            currentFormat = currentFormat,
            currentPlayCount = song.playCount?.fastSumBy { it.count } ?: 0,
            clipboardManager = clipboardManager,
            setVisibility = { showDetailsDialog = it }
        )
    }
}

@Composable
fun ImportSongsMenu(
    items: MutableList<String>,
    importM3uViewModel: ImportM3uViewModel,
    onDismiss: () -> Unit,
) {

    GridMenu(
        contentPadding = PaddingValues(
            start = 8.dp,
            top = 8.dp,
            end = 8.dp,
            bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()
        )
    ) {

        GridMenuItem(
            icon = Icons.Rounded.Delete,
            title = R.string.delete_import
        ) {
            onDismiss()

            items.forEach { uuid ->
                importM3uViewModel.importedSongs.remove(
                    importM3uViewModel.importedSongs.find{
                    it.uuid == uuid
                })
            }
            items.clear()
        }
    }

}

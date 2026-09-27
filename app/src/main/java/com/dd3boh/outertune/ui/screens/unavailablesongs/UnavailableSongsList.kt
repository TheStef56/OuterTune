package com.dd3boh.outertune.ui.screens.unavailablesongs

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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dd3boh.outertune.LocalDatabase
import com.dd3boh.outertune.LocalMenuState
import com.dd3boh.outertune.LocalPlayerAwareWindowInsets
import com.dd3boh.outertune.R
import com.dd3boh.outertune.constants.AlbumThumbnailSize
import com.dd3boh.outertune.constants.ThumbnailCornerRadius
import com.dd3boh.outertune.ui.component.LazyColumnScrollbar
import com.dd3boh.outertune.ui.component.items.UnavailableSongListItem
import com.dd3boh.outertune.ui.menu.UnavailableSongMenu
import com.dd3boh.outertune.viewmodels.UnavailableSongsViewModel

@Composable
fun UnavailableSongsList(
    navController: NavController,
    viewModel: UnavailableSongsViewModel,
    selection: MutableList<String>
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val windowInsets = LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime)
    val mainListState = rememberSaveable(saver = LazyListState.Saver) {
        LazyListState()
    }

    val haptic = LocalHapticFeedback.current

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
                                context = context,
                                onPercentageChange = {
                                    viewModel.percentage.intValue = it
                                },
                                onLoadingChange = {
                                    viewModel.isLoading.value = it
                                })
                        },
                        enabled = viewModel.unavailableSongs.isNotEmpty()
                    ) {
                        Text(stringResource(R.string.replace_all_with_autosearch))
                    }
                }
            }
        }
        if (viewModel.isLoading.value) {
            item {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = if (viewModel.unavailableSongs.isEmpty()) 210.dp else 0.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(80.dp)
                    ) {
                        Text(
                            text = "${viewModel.percentage.intValue}%",
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
                key = { _, (_, _, uuid) -> uuid }
            ) { index, (query, song, uuid) ->
                UnavailableSongListItem (
                    song = song,
                    isMissing = false,
                    inSelectMode = viewModel.inSelectMode.value,
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
                                onSwapClick = {
                                    viewModel.onSearchQueryChange(TextFieldValue(query))
                                },
                                onDismiss = menuState::dismiss
                            )
                        }
                        viewModel.toSwapIndex.intValue = index
                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                        viewModel.onSearchQueryChange(TextFieldValue(query))
                    },
                    modifier = Modifier
                        .background (
                            MaterialTheme.colorScheme.background
                        )
                        .combinedClickable(
                            onClick = {
                                if (viewModel.inSelectMode.value) {
                                    if (selection.contains(uuid)) {
                                        selection.remove(uuid)
                                    } else {
                                        selection.add(uuid)
                                    }
                                }
                            },
                            onLongClick = {
                                if (!viewModel.inSelectMode.value) {
                                    viewModel.inSelectMode.value = true
                                    selection.add(uuid)
                                }
                            }
                        )

                )
            }
        }
    }
    LazyColumnScrollbar(
        state = mainListState,
    )
}
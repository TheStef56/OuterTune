package com.dd3boh.outertune.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dd3boh.outertune.utils.reportException
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.utils.completed
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor() : ViewModel() {
    val playlists = MutableStateFlow<List<PlaylistItem>?>(null)
    val albums = MutableStateFlow<List<AlbumItem>?>(null)
    val artists = MutableStateFlow<List<ArtistItem>?>(null)
    val isLoading = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            YouTube.library("FEmusic_liked_playlists").completed().onSuccess {
                playlists.value = it.items.filterIsInstance<PlaylistItem>()
                isLoading.value += 1
            }.onFailure {
                reportException(it)
                isLoading.value += 1
            }
            YouTube.library("FEmusic_liked_albums").completed().onSuccess {
                albums.value = it.items.filterIsInstance<AlbumItem>()
                isLoading.value += 1
            }.onFailure {
                reportException(it)
                isLoading.value += 1
            }
            YouTube.library("FEmusic_library_corpus_artists").completed().onSuccess {
                artists.value = it.items.filterIsInstance<ArtistItem>()
                isLoading.value += 1
            }.onFailure {
                reportException(it)
                isLoading.value += 1
            }
        }
    }
}

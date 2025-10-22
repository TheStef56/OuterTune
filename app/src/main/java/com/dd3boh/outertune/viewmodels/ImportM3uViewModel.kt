package com.dd3boh.outertune.viewmodels

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dd3boh.outertune.db.MusicDatabase
import com.dd3boh.outertune.db.entities.Song
import com.dd3boh.outertune.models.ItemsPage
import com.dd3boh.outertune.ui.screens.ImportM3uFilter
import com.dd3boh.outertune.utils.reportException
import com.zionhuang.innertube.YouTube
import com.zionhuang.innertube.models.SongItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ImportM3uViewModel @Inject constructor(
    database: MusicDatabase,
): ViewModel() {
    val scope = CoroutineScope(Dispatchers.IO)
    val importedSongs = mutableStateListOf<ImportedSong>()
    var onlineResult = MutableStateFlow<ItemsPage?>(null)


    val query = MutableStateFlow("")


    val localResult: Flow<List<Song>> = query.flatMapLatest { query ->
        if (query.isEmpty()) {
            flowOf(emptyList<Song>())
        } else {
            database.searchSongs(query)

        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())



    fun search(query: String) {
        scope.launch {
            onlineResult.value = null
            val suggestions = YouTube.searchSuggestions(query).getOrNull()
            val items =
                suggestions?.recommendedItems.orEmpty().distinctBy { it.id }.filter { it is SongItem }.toMutableList()
            YouTube.search(query, YouTube.SearchFilter.FILTER_SONG)
                .onSuccess { result ->
                    items += result.items
                    onlineResult.value = ItemsPage(items.distinctBy { it.id }, result.continuation)
                }
                .onFailure {
                    reportException(it)
                }
        }
    }

    fun loadMore() {
        scope.launch {
            val viewState = onlineResult.value ?: return@launch
            val continuation = viewState.continuation
            if (continuation != null) {
                val searchResult =
                    YouTube.searchContinuation(continuation).getOrNull()
                        ?: return@launch
                this@ImportM3uViewModel.onlineResult.value = ItemsPage(
                    (viewState.items + searchResult.items).distinctBy { it.id },
                    searchResult.continuation
                )
            }
        }

    }
}

data class ImportedSong(
    val query:String,
    val song: Song,
    val uuid: String,
    val status: ImportM3uFilter
)
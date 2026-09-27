package com.dd3boh.outertune.viewmodels

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import com.dd3boh.outertune.constants.SearchSource
import com.dd3boh.outertune.db.MusicDatabase
import com.dd3boh.outertune.db.entities.Song
import com.dd3boh.outertune.models.ItemsPage
import com.dd3boh.outertune.utils.reportException
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.distinctBy
import kotlin.collections.emptyList
import kotlin.collections.orEmpty

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class UnavailableSongsViewModel @Inject constructor(
    private val database: MusicDatabase,
) : ViewModel() {

    val scope = CoroutineScope(Dispatchers.IO)

    val unavailableSongs = mutableStateListOf<UnavailableSong>()

    // -------------------------
    // Shared UI state
    // -------------------------

    var toSwapIndex = mutableIntStateOf(0)

    var searchSource = mutableStateOf(SearchSource.ONLINE)

    var percentage = mutableIntStateOf(0)

    var inSelectMode = mutableStateOf(false)

    val selection = mutableStateListOf<String>()

    var isLoading = mutableStateOf(false)

    var searchQuery = mutableStateOf(TextFieldValue())

    var localResult: Flow<List<Song>> = flowOf(emptyList<Song>())
    var onlineResult = MutableStateFlow<ItemsPage?>(null)

    fun onSearchQueryChange(value: TextFieldValue) {
        searchQuery.value = value

        if (searchSource.value == SearchSource.ONLINE) {
            search(searchQuery.value.text)
        } else {
            localResult =
                if (searchQuery.value.text.isEmpty()) {
                    flowOf(emptyList<Song>())
                } else {
                    database.searchSongs(searchQuery.value.text)
                }
        }
    }

    // -------------------------
    // Search
    // -------------------------

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
                this@UnavailableSongsViewModel.onlineResult.value = ItemsPage(
                    (viewState.items + searchResult.items).distinctBy { it.id },
                    searchResult.continuation
                )
            }
        }

    }

}

data class UnavailableSong(
    val query: String,
    val song: Song,
    val uuid: String,
)
package com.dd3boh.outertune.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dd3boh.outertune.models.ItemsPage
import com.dd3boh.outertune.utils.reportException
import com.zionhuang.innertube.YouTube
import com.zionhuang.innertube.models.SongItem
import com.zionhuang.innertube.pages.SearchSummaryPage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.distinctBy
import kotlin.collections.orEmpty

@HiltViewModel
class OnlineSearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val query = savedStateHandle.get<String>("query")!!
    val route = savedStateHandle.get<Boolean>("rep") ?: false // ← optional route key if passed
    val filter = MutableStateFlow<YouTube.SearchFilter?>(null)
    var summaryPage by mutableStateOf<SearchSummaryPage?>(null)
    val viewStateMap = mutableStateMapOf<String, ItemsPage?>()

    init {
        viewModelScope.launch {
            if (route) {
                if (viewStateMap[YouTube.SearchFilter.FILTER_SONG.value] == null) {
                    val suggestions = YouTube.searchSuggestions(query).getOrNull()
                    val items = suggestions?.recommendedItems.orEmpty().distinctBy { it.id }.filter{it is SongItem}.toMutableList()
                    YouTube.search(query, YouTube.SearchFilter.FILTER_SONG)
                        .onSuccess { result ->
                            items += result.items
                            viewStateMap[YouTube.SearchFilter.FILTER_SONG.value] =
                                ItemsPage(items.distinctBy { it.id }, result.continuation)
                        }
                        .onFailure {
                            reportException(it)
                        }
                }
            } else {
                filter.collect { filter ->
                    if (filter == null) {
                        if (summaryPage == null) {
                            YouTube.searchSummary(query)
                                .onSuccess {
                                    summaryPage = it
                                }
                                .onFailure {
                                    reportException(it)
                                }
                        }
                    } else {
                        if (viewStateMap[filter.value] == null) {
                            YouTube.search(query, filter)
                                .onSuccess { result ->
                                    viewStateMap[filter.value] = ItemsPage(
                                        result.items.distinctBy { it.id },
                                        result.continuation
                                    )
                                }
                                .onFailure {
                                    reportException(it)
                                }
                        }
                    }
                }
            }
        }
    }
    fun loadMore() {
        viewModelScope.launch {
            if (route) {
                val filter = YouTube.SearchFilter.FILTER_SONG.value
                viewModelScope.launch {
                    val viewState = viewStateMap[filter] ?: return@launch
                    val continuation = viewState.continuation
                    if (continuation != null) {
                        val searchResult =
                            YouTube.searchContinuation(continuation).getOrNull()
                                ?: return@launch
                        viewStateMap[filter] = ItemsPage(
                            (viewState.items + searchResult.items).distinctBy { it.id },
                            searchResult.continuation
                        )
                    }
                }
            } else {
                val filter = filter.value?.value
                if (filter == null) return@launch
                val viewState = viewStateMap[filter] ?: return@launch
                val continuation = viewState.continuation
                if (continuation != null) {
                    val searchResult =
                        YouTube.searchContinuation(continuation).getOrNull() ?: return@launch
                    viewStateMap[filter] = ItemsPage(
                        (viewState.items + searchResult.items).distinctBy { it.id },
                        searchResult.continuation
                    )

                }
            }
        }
    }
}

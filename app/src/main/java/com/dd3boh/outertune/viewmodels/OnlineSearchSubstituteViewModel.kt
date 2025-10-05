package com.dd3boh.outertune.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dd3boh.outertune.models.ItemsPage
import com.dd3boh.outertune.ui.menu.YouTubeSongMenu
import com.dd3boh.outertune.utils.reportException
import com.zionhuang.innertube.YouTube
import com.zionhuang.innertube.pages.SearchSummaryPage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.plus

@HiltViewModel
class OnlineSearchSubstituteViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val query = savedStateHandle.get<String>("query")!!
    val viewStateMap = mutableStateMapOf<String, ItemsPage?>()

    init {
        viewModelScope.launch {
            if (viewStateMap[YouTube.SearchFilter.FILTER_SONG.value] == null) {
                YouTube.search(query, YouTube.SearchFilter.FILTER_SONG)
                    .onSuccess { result ->
                        viewStateMap[YouTube.SearchFilter.FILTER_SONG.value] = ItemsPage(result.items.distinctBy { it.id }, result.continuation)
                    }
                    .onFailure {
                        reportException(it)
                }
            }
        }
    }
    fun loadMore() {
        val filter = YouTube.SearchFilter.FILTER_SONG.value
        viewModelScope.launch {
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


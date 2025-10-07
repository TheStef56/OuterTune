package com.dd3boh.outertune.viewmodels

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.dd3boh.outertune.db.entities.Song

class ImportM3uViewModel : ViewModel() {
    val importedSongs = mutableStateListOf<Pair<String, Song>>()
    val rejectedSongs = mutableStateListOf<Pair<Int, Song>>()
}
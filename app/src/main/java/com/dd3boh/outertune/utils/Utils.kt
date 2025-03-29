/*
 * Copyright (C) 2025 O​u​t​er​Tu​ne Project
 *
 * SPDX-License-Identifier: GPL-3.0
 *
 * For any other attributions, refer to the git commit history
 */

package com.dd3boh.outertune.utils

import android.content.pm.PackageManager
import com.dd3boh.outertune.ui.screens.settings.LibraryFilter

fun reportException(throwable: Throwable) {
    throwable.printStackTrace()
}

/**
 * Converts the enable filters list (string) to LibraryFilter
 *
 * @param str Encoded string
 */
fun decodeFilterString(str: String): List<LibraryFilter> {
    return str.toCharArray().map {
        when (it) {
            'A' -> LibraryFilter.ALBUMS
            'R' -> LibraryFilter.ARTISTS
            'P' -> LibraryFilter.PLAYLISTS
            'S' -> LibraryFilter.SONGS
            'F' -> LibraryFilter.FOLDERS
            'L' -> LibraryFilter.ALL
            else -> LibraryFilter.ALL
        }
    }.distinct()
}

/**
 * Converts the LibraryFilter filters list to string
 *
 * @param list Decoded LibraryFilter list
 */
fun encodeFilterString(list: List<LibraryFilter>): String {
    var encoded = ""
    list.forEach {
        encoded += when (it) {
            LibraryFilter.ALBUMS -> "A"
            LibraryFilter.ARTISTS -> "R"
            LibraryFilter.PLAYLISTS -> "P"
            LibraryFilter.SONGS -> "S"
            LibraryFilter.FOLDERS -> "F"
            LibraryFilter.ALL -> "L"
        }
    }
    return encoded
}


/**
 * Check if a package with the specified package name is installed
 */
fun isPackageInstalled(packageName: String, packageManager: PackageManager): Boolean {
    return try {
        packageManager.getPackageInfo(packageName, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}
package com.dd3boh.outertune.constants

import android.os.Build
import com.dd3boh.outertune.BuildConfig

/**
 * Feature flags
 */

const val ENABLE_UPDATE_CHECKER = BuildConfig.FLAVOR == "full"

const val ENABLE_FFMETADATAEX = BuildConfig.FLAVOR == "full"


/**
 * Extra configuration
 */

// Maximum concurrent image resolution jobs
const val MAX_COIL_JOBS = 16

// maximum parallel download jobs allowed
const val MAX_CONCURRENT_DOWNLOAD_JOBS = 3 // ytm defaults to 3

// maximum parallel scanner jobs allowed
const val MAX_LM_SCANNER_JOBS = 4

// maximum parallel scanner jobs allowed
const val MAX_YTM_SYNC_JOBS = 3


/**
 * Constants
 */
const val LYRIC_FETCH_TIMEOUT = 60000L
const val SNACKBAR_VERY_SHORT = 2000L
const val OOBE_VERSION = 5
const val SCANNER_OWNER_DL = 32
const val SCANNER_OWNER_LM = 1
const val SCANNER_OWNER_M3U = 2
const val SYNC_CD = 60000 * 30

val DEFAULT_PLAYER_BACKGROUND =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PlayerBackgroundStyle.BLUR else PlayerBackgroundStyle.GRADIENT

/**
 * Debug
 */
// crash at first extractor scanner error. Currently not implemented
const val SCANNER_CRASH_AT_FIRST_ERROR = false

// true will not use multithreading for scanner
const val SYNC_SCANNER = false

// enable verbose debugging details for scanner
const val SCANNER_DEBUG = false

// enable verbose debugging details for extractor
const val EXTRACTOR_DEBUG = false

// enable printing of *ALL* data that extractor reads
const val DEBUG_SAVE_OUTPUT = false // ignored (will be false) when EXTRACTOR_DEBUG IS false

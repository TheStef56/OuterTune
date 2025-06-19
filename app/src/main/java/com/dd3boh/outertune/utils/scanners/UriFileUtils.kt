package com.dd3boh.outertune.utils.scanners

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import java.io.File

fun documentFileFromUri(context: Context, uris: List<Uri>): List<DocumentFile> {
    return uris
        .mapNotNull { DocumentFile.fromTreeUri(context, it) }
        .filter { it.isDirectory }
}

fun documentFileFromUri(context: Context, uri: Uri): DocumentFile? {
    return DocumentFile.fromTreeUri(context, uri)
}

fun stringFromUriList(uris: List<Uri>): String {
    return uris.joinToString("\n")
}

fun uriListFromString(str: String): List<Uri> {
    return str.split("\n").map { it.toUri() }.filter { it.toString().isNotBlank() }
}

fun fileFromUri(context: Context, uri: Uri): File? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        if (!DocumentsContract.isTreeUri(uri)) return null
        if (uri.authority != "com.android.externalstorage.documents") return null

        val treeDocId = DocumentsContract.getDocumentId(uri)
        val rootId: String
        val relativePath: String

        if (treeDocId.contains(":")) {
            val parts = treeDocId.split(":", limit = 2)
            rootId = parts[0]
            relativePath = parts[1]
        } else {
            rootId = treeDocId
            relativePath = ""
        }

        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager

        val rootDir = if (rootId.equals("primary", ignoreCase = true)) {
            storageManager.primaryStorageVolume.directory
        } else {
            storageManager.storageVolumes.firstOrNull {
                it.uuid != null && it.uuid.equals(rootId, ignoreCase = true)
            }?.directory
        }

        return rootDir?.let { if (relativePath.isEmpty()) it else File(it, relativePath) }
    } else {
        // TODO: test this even works on lower sdk
        if (!DocumentsContract.isDocumentUri(context, uri)) return null

        if (uri.authority != "com.android.externalstorage.documents") return null

        val docId = DocumentsContract.getDocumentId(uri)
        val parts = docId.split(":")

        if (parts.size < 2) return null

        val type = parts[0]
        val relativePath = parts[1]

        val rootDir = when (type.lowercase()) {
            "primary" -> Environment.getExternalStorageDirectory()
            else -> {
                // Try to handle secondary storage
                val secondaryStorage = System.getenv("SECONDARY_STORAGE")?.split(":")
                secondaryStorage?.firstOrNull { File(it).exists() }?.let { File(it) }
            }
        }

        return rootDir?.let { File(it, relativePath) }
    }
}

fun absoluteFilePathFromUri(context: Context, uri: Uri): String? {
    val dfUri = documentFileFromUri(context, uri)?.uri
    if (dfUri == null) return null
    return fileFromUri(context, dfUri)?.absolutePath
}
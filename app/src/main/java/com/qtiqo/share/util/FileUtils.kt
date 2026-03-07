package com.qtiqo.share.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

data class FileMeta(
    val uri: Uri,
    val name: String,
    val mimeType: String?,
    val sizeBytes: Long
)

fun resolveFileMeta(context: Context, uri: Uri): FileMeta {
    val resolver = context.contentResolver
    var name = "file"
    var size = 0L
    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
        ?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex >= 0) name = cursor.getString(nameIndex) ?: name
                if (sizeIndex >= 0) size = cursor.getLong(sizeIndex)
            }
        }
    if (size <= 0L) {
        resolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
            if (descriptor.length > 0L) size = descriptor.length
        }
    }
    if (size <= 0L) {
        resolver.openInputStream(uri)?.use { input ->
            val buffer = ByteArray(8 * 1024)
            var total = 0L
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
            }
            if (total > 0L) size = total
        }
    }
    require(size > 0L) { "Unable to determine file size for selected file" }
    return FileMeta(
        uri = uri,
        name = name,
        mimeType = resolver.getType(uri),
        sizeBytes = size
    )
}

fun ContentResolver.persistReadPermission(uri: Uri) {
    try {
        takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    } catch (_: SecurityException) {
        // Some providers do not allow persistable permissions.
    }
}

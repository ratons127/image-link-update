package com.qtiqo.share.data.remote

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import okhttp3.MediaType.Companion.toMediaTypeOrNull

class ProgressRequestBody(
    private val contentType: String?,
    private val contentLengthBytes: Long,
    private val writer: suspend (BufferedSink, (Long) -> Unit) -> Unit,
    private val onProgress: (Long, Long) -> Unit
) : RequestBody() {
    override fun contentType(): MediaType? = contentType?.toMediaTypeOrNullCompat()

    override fun contentLength(): Long = contentLengthBytes

    override fun writeTo(sink: BufferedSink) {
        throw UnsupportedOperationException("Use coroutine-based upload helper instead")
    }
}

private fun String.toMediaTypeOrNullCompat(): MediaType? = toMediaTypeOrNull()

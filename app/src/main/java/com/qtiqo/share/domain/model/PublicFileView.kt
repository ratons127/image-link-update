package com.qtiqo.share.domain.model

data class PublicFileView(
    val fileId: String,
    val name: String,
    val mimeType: String?,
    val sizeBytes: Long,
    val localUri: String?,
    val shareToken: String,
    val allowDownloads: Boolean,
    val isImage: Boolean,
    val isRevoked: Boolean
)

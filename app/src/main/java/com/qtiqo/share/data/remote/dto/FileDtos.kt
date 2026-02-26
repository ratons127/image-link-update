package com.qtiqo.share.data.remote.dto

data class InitFileUploadRequest(
    val fileName: String,
    val mimeType: String?,
    val sizeBytes: Long,
    val privacy: String,
    val downloadEnabled: Boolean
)

data class InitFileUploadResponse(
    val fileId: String,
    val uploadUrl: String,
    val shareToken: String
)

data class CompleteFileUploadRequest(
    val fileId: String
)

data class FileDto(
    val id: String,
    val name: String,
    val mimeType: String?,
    val sizeBytes: Long,
    val privacy: String,
    val downloadEnabled: Boolean,
    val shareToken: String?,
    val shareUrl: String?,
    val createdAt: Long,
    val uploadedAt: Long?,
    val revoked: Boolean
)

data class PagedFilesResponse(
    val items: List<FileDto>,
    val page: Int,
    val pageSize: Int,
    val total: Int
)

data class PatchFileRequest(
    val privacy: String? = null,
    val downloadEnabled: Boolean? = null
)

data class RevokeRegenerateResponse(
    val shareToken: String,
    val shareUrl: String
)

data class PublicFileResponse(
    val id: String,
    val name: String,
    val mimeType: String?,
    val sizeBytes: Long,
    val shareToken: String,
    val allowDownloads: Boolean,
    val revoked: Boolean,
    val viewUrl: String? = null,
    val downloadUrl: String? = null
)

package com.qtiqo.share.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.qtiqo.share.domain.model.FilePrivacy
import com.qtiqo.share.domain.model.UploadStatus

@Entity(tableName = "uploads")
data class UploadEntity(
    @PrimaryKey val id: String,
    val ownerIdentifier: String,
    val localUri: String,
    val fileName: String,
    val mimeType: String?,
    val sizeBytes: Long,
    val status: UploadStatus,
    val progress: Int,
    val shareToken: String?,
    val shareUrl: String?,
    val privacy: FilePrivacy,
    val downloadEnabled: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val uploadedAt: Long?,
    val revoked: Boolean,
    val remoteFileId: String? = null,
    val errorMessage: String? = null
)

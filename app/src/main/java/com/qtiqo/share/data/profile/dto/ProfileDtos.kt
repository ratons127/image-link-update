package com.qtiqo.share.data.profile.dto

data class MeSummaryDto(
    val filesCount: Int,
    val storageUsedBytes: Long,
    val storageLimitBytes: Long,
    val uploadLimitBytes: Long
)

data class ChangePasswordRequestDto(
    val currentPassword: String,
    val newPassword: String
)

data class LogoutRequestDto(
    val token: String? = null
)


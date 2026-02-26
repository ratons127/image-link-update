package com.qtiqo.share.feature.profile.domain.models

data class ProfileSummary(
    val filesCount: Int,
    val storageUsedBytes: Long,
    val storageLimitBytes: Long,
    val uploadLimitBytes: Long
)

data class ProfileSession(
    val userId: String,
    val email: String,
    val token: String
)


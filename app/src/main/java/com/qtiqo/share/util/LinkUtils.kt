package com.qtiqo.share.util

import java.util.UUID

const val SHARE_LINK_BASE = "https://imagelink.qtiqo.com"

fun generateShareToken(): String = UUID.randomUUID().toString().replace("-", "").take(16)

fun shareUrlForToken(token: String): String = "$SHARE_LINK_BASE/s/$token"

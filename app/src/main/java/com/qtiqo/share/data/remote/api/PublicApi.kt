package com.qtiqo.share.data.remote.api

import com.qtiqo.share.data.remote.dto.PublicFileResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface PublicApi {
    @GET("/public/{shareToken}")
    suspend fun getPublicFile(@Path("shareToken") shareToken: String): PublicFileResponse
}

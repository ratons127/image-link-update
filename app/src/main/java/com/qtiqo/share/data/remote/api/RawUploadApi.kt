package com.qtiqo.share.data.remote.api

import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.PUT
import retrofit2.http.Url

interface RawUploadApi {
    @PUT
    suspend fun putFile(@Url uploadUrl: String, @Body body: RequestBody): Response<Unit>
}

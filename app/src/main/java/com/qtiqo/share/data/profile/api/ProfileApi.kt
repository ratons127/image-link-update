package com.qtiqo.share.data.profile.api

import com.qtiqo.share.data.profile.dto.ChangePasswordRequestDto
import com.qtiqo.share.data.profile.dto.LogoutRequestDto
import com.qtiqo.share.data.profile.dto.MeSummaryDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface ProfileApi {
    @GET("/me/summary")
    suspend fun getMeSummary(@Header("Authorization") bearer: String): MeSummaryDto

    @POST("/me/change-password")
    suspend fun changePassword(
        @Header("Authorization") bearer: String,
        @Body request: ChangePasswordRequestDto
    )

    @POST("/auth/logout")
    suspend fun logout(
        @Header("Authorization") bearer: String,
        @Body request: LogoutRequestDto
    )
}

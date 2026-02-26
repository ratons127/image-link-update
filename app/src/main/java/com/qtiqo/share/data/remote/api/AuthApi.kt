package com.qtiqo.share.data.remote.api

import com.qtiqo.share.data.remote.dto.AuthResponse
import com.qtiqo.share.data.remote.dto.ForgotPasswordRequest
import com.qtiqo.share.data.remote.dto.LoginRequest
import com.qtiqo.share.data.remote.dto.LogoutRequest
import com.qtiqo.share.data.remote.dto.SignUpRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("/auth/signup")
    suspend fun signUp(@Body request: SignUpRequest): AuthResponse

    @POST("/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("/auth/forgot")
    suspend fun forgot(@Body request: ForgotPasswordRequest)

    @POST("/auth/logout")
    suspend fun logout(@Body request: LogoutRequest)
}
